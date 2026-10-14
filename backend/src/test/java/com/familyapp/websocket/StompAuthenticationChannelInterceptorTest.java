package com.familyapp.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.auth.security.JwtService;
import com.familyapp.consensus.api.OutingMembershipPort;

class StompAuthenticationChannelInterceptorTest {
    private final MessageChannel channel = (message, timeout) -> true;
    private final OutingMembershipPort outingMembershipPort = mock(OutingMembershipPort.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID familyId = UUID.randomUUID();
    private final UUID outingId = UUID.randomUUID();
    private final JwtService jwtService = new JwtService(
            Base64.getEncoder().encodeToString(new byte[32]),
            3600);
    private StompAuthenticationChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthenticationChannelInterceptor(jwtService, outingMembershipPort);
    }

    @Test
    void authenticatesConnectAndAllowsAnAttendeeToSubscribe() {
        StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectAccessor.addNativeHeader(
                "Authorization", "Bearer " + jwtService.issueToken(userId, familyId));
        Message<byte[]> connectMessage = MessageBuilder.createMessage(
                new byte[0], connectAccessor.getMessageHeaders());

        Message<?> authenticatedMessage =
                interceptor.preSend(connectMessage, Objects.requireNonNull(channel));
        StompHeaderAccessor authenticatedAccessor =
                Objects.requireNonNull(
                        MessageHeaderAccessor.getAccessor(authenticatedMessage, StompHeaderAccessor.class));
        Authentication authentication = (Authentication) Objects.requireNonNull(authenticatedAccessor.getUser());
        FamilyPrincipal principal = (FamilyPrincipal) Objects.requireNonNull(authentication.getPrincipal());

        assertEquals(userId, principal.userId());
        assertEquals(familyId, principal.familyId());

        StompHeaderAccessor subscribeAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribeAccessor.setDestination("/topic/outings/" + outingId + "/votes");
        subscribeAccessor.setUser(authentication);
        Message<byte[]> subscribeMessage = MessageBuilder.createMessage(
                new byte[0], subscribeAccessor.getMessageHeaders());
        when(outingMembershipPort.isAttendee(outingId, familyId, userId)).thenReturn(true);

        interceptor.preSend(subscribeMessage, Objects.requireNonNull(channel));

        verify(outingMembershipPort).isAttendee(outingId, familyId, userId);
    }

    @Test
    void rejectsNonAttendeeSubscriptionsAndClientSends() {
        Authentication authentication = new org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken(
                        new FamilyPrincipal(userId, familyId), null, java.util.List.of());
        StompHeaderAccessor subscribeAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribeAccessor.setDestination("/topic/outings/" + outingId + "/votes");
        subscribeAccessor.setUser(authentication);
        Message<byte[]> subscribeMessage = MessageBuilder.createMessage(
                new byte[0], subscribeAccessor.getMessageHeaders());
        when(outingMembershipPort.isAttendee(outingId, familyId, userId)).thenReturn(false);

        assertThrows(
                MessageDeliveryException.class,
                () -> interceptor.preSend(subscribeMessage, Objects.requireNonNull(channel)));

        StompHeaderAccessor sendAccessor = StompHeaderAccessor.create(StompCommand.SEND);
        sendAccessor.setDestination("/topic/outings/" + outingId + "/votes");
        Message<byte[]> sendMessage = MessageBuilder.createMessage(
                new byte[0], sendAccessor.getMessageHeaders());
        assertThrows(
                MessageDeliveryException.class,
                () -> interceptor.preSend(sendMessage, Objects.requireNonNull(channel)));
    }
}
