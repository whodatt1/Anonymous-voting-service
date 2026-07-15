package com.instance.vote.controller;

import com.instance.vote.annotation.RateLimit;
import com.instance.vote.dto.VoteRequest;
import com.instance.vote.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @RateLimit(limit = 5, windowSeconds = 60) // 1분에 5번
    @PostMapping("/{shareCode}/vote")
    public ResponseEntity<Void> castVote(
            @PathVariable String shareCode,
            @RequestBody @Valid VoteRequest.Body request,
            @CookieValue(name = "participantToken", required = true) String participantToken) {

        voteService.castVote(shareCode, new VoteRequest.Cast(request.optionId(), participantToken));
        return ResponseEntity.noContent().build();
    }
}
