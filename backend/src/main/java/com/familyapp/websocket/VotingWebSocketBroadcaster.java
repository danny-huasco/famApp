package com.familyapp.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import com.familyapp.event.VoteTallyUpdatedEvent;

@Component
public class VotingWebSocketBroadcaster {
    private final SimpMessagingTemplate messagingTemplate;

    public VotingWebSocketBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void broadcastTally(VoteTallyUpdatedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/outings/" + event.outingId() + "/votes",
                event);
    }
}
