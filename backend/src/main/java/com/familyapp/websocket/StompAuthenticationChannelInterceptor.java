package com.familyapp.websocket;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.auth.security.JwtService;
import com.familyapp.consensus.api.OutingMembershipPort;

@Component
public class StompAuthenticationChannelInterceptor implements ChannelInterceptor {
    private static final Pattern VOTE_TOPIC =
            Pattern.compile("^/topic/outings/([0-9a-fA-F-]{36})/votes$");

    private final JwtService jwtService;
    private final OutingMembershipPort outingMembershipPort;

    public StompAuthenticationChannelInterceptor(
            JwtService jwtService,
            OutingMembershipPort outingMembershipPort) {
        this.jwtService = jwtService;
        this.outingMembershipPort = outingMembershipPort;
    }

    @Override
    @NonNull
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticate(message, accessor);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            authorizeVoteSubscription(message, accessor);
        } else if (accessor.getCommand() == StompCommand.SEND) {
            throw new MessageDeliveryException(message, "Client STOMP sends are not supported.");
        }
        return message;
    }

    private void authenticate(@NonNull Message<?> message, @NonNull StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MessageDeliveryException(message, "A bearer token is required to connect.");
        }

        try {
            Claims claims = jwtService.parseToken(authorization.substring("Bearer ".length()).trim());
            String familyIdClaim = claims.get("family_id", String.class);
            if (familyIdClaim == null || familyIdClaim.isBlank()) {
                throw new IllegalArgumentException("JWT family claim is missing.");
            }
            FamilyPrincipal principal = new FamilyPrincipal(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(familyIdClaim));
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    principal, null, List.of()));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new MessageDeliveryException(message, "The STOMP bearer token is invalid.", exception);
        }
    }

    private void authorizeVoteSubscription(
            @NonNull Message<?> message,
            @NonNull StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof FamilyPrincipal principal)) {
            throw new MessageDeliveryException(message, "Authenticate before subscribing.");
        }

        String destination = accessor.getDestination();
        Matcher matcher = destination == null ? null : VOTE_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new MessageDeliveryException(message, "Only outing vote topics are available.");
        }

        UUID outingId;
        try {
            outingId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException exception) {
            throw new MessageDeliveryException(message, "The outing topic is invalid.", exception);
        }
        if (!outingMembershipPort.isAttendee(outingId, principal.familyId(), principal.userId())) {
            throw new MessageDeliveryException(message, "Only outing attendees can subscribe.");
        }
    }
}
