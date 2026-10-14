package com.familyapp.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.familyapp.event.VoteTallyUpdatedEvent;

class VotingWebSocketBroadcasterTest {
    @Test
    void sendsUpdatedFirstPreferenceCountsToTheOutingVoteTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        VotingWebSocketBroadcaster broadcaster = new VotingWebSocketBroadcaster(messagingTemplate);
        UUID outingId = UUID.randomUUID();
        VoteTallyUpdatedEvent event = new VoteTallyUpdatedEvent(
                outingId,
                3,
                Map.of(UUID.randomUUID(), 2, UUID.randomUUID(), 1));

        broadcaster.broadcastTally(event);

        verify(messagingTemplate).convertAndSend("/topic/outings/" + outingId + "/votes", event);
    }
}
