package com.instance.vote.event;

import com.instance.vote.dto.PollResponse;
import com.instance.vote.service.PollService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SseEventHandler {

    private final SseEmitterManager sseEmitterManager;
    private final PollService pollService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVoteCast(VoteCastEvent event) {
        PollResponse.Detail detail = pollService.getPoll(event.shareCode());
        sseEmitterManager.broadcast(event.pollId(), detail.options());
    }
}
