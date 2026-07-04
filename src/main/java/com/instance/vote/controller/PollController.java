package com.instance.vote.controller;

import com.instance.vote.dto.PollRequest;
import com.instance.vote.dto.PollResponse;
import com.instance.vote.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/votes")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping
    public ResponseEntity<PollResponse.Create> createPoll(@RequestBody @Valid PollRequest.Create request) {
        PollResponse.Create response = pollService.createPoll(request);
        URI location = URI.create("/votes/" + response.shareCode());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{shareCode}")
    public ResponseEntity<PollResponse.Detail> getPoll(
            @PathVariable String shareCode,
            @CookieValue(name = "participantToken", required = false) String exstingToken) {

        PollResponse.Detail response = pollService.getPoll(shareCode, exstingToken);

        if (exstingToken != null) {
            return ResponseEntity.ok(response); // 쿠키가 있다면 바로 반환
        }

        String participantToken = UUID.randomUUID().toString();

        // 만료기간 고려하여 동적 maxAge 계산
        long secondsUntilExpiry = ChronoUnit.SECONDS.between(LocalDateTime.now(), response.expiresAt());

        ResponseCookie cookie = ResponseCookie.from("participantToken", participantToken)
                .httpOnly(true)
                .path("/")
                // 현재는 만료된 getPoll도 정상 반환하게 되어있다.
                // Duration.ofSeconds(-1)을 넘기면 예외가 발생하므로 0으로 방어
                .maxAge(Duration.ofSeconds(Math.max(secondsUntilExpiry, 0)))
                // 접속 url을 제공한다는 정책상 Lax 채택
                // 다른 사이트(카카오톡 등)에서 링크를 타고 오는 GET요청은 허용 POST PUT 차단
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PatchMapping("/{shareCode}/close")
    public ResponseEntity<Void> closePoll(
            @PathVariable String shareCode,
            @RequestParam String hostToken) {
        pollService.closePoll(shareCode, hostToken);
        return ResponseEntity.noContent().build();
    }
}
