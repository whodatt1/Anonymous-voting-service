package com.instance.vote.controller;

import com.instance.vote.dto.PollResponse;
import com.instance.vote.event.SseEmitterManager;
import com.instance.vote.service.PollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/votes")
@RequiredArgsConstructor
public class SseController {

    private final PollService pollService;
    private final SseEmitterManager sseEmitterManager;

    @GetMapping(value = "/{shareCode}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(
            @PathVariable String shareCode,
            @RequestParam String hostToken) {
        PollResponse.Detail detail = pollService.validateSseConnection(shareCode, hostToken);

        long timeout = Duration.between(LocalDateTime.now(), detail.expiresAt()).toMillis();
        SseEmitter emitter = sseEmitterManager.register(detail.pollId(), timeout);

        try {
            emitter.send(SseEmitter.event().data(detail.options()));
        } catch (IOException e) {
            emitter.completeWithError(e); // onError 콜백 발화 -> SseEmitterManager.remove() 호출
            return ResponseEntity.ok().body(emitter);
        }

        return ResponseEntity.ok().body(emitter);
    }
}
