package com.instance.vote.controller;

import com.instance.vote.annotation.RateLimit;
import com.instance.vote.domain.Poll;
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

    @RateLimit(limit = 5, windowSeconds = 60) // 스팸 생성 방지
    @PostMapping
    public ResponseEntity<PollResponse.Create> createPoll(@RequestBody @Valid PollRequest.Create request) {
        PollResponse.Create response = pollService.createPoll(request);
        URI location = URI.create("/votes/" + response.shareCode());

        // 만료기간 고려하여 동적 maxAge 계산
        long secondsUntilExpiry = ChronoUnit.SECONDS.between(LocalDateTime.now(), response.expiresAt());

        ResponseCookie cookie = ResponseCookie.from("hostToken", response.hostToken())
                .httpOnly(true)
                .path("/votes/" + response.shareCode())
                // Duration.ofSeconds(-1)을 넘기면 예외가 발생하므로 0으로 방어
                .maxAge(Duration.ofSeconds(Math.max(secondsUntilExpiry, 0)))
                .sameSite("Strict")
                .build();

        return ResponseEntity.created(location)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @GetMapping("/{shareCode}/host")
    public ResponseEntity<PollResponse.Detail> getHostPoll(
            @PathVariable String shareCode,
            @CookieValue(name = "hostToken") String hostToken) {
        PollResponse.Detail response = pollService.getHostPoll(shareCode, hostToken);

        return ResponseEntity.ok()
                .body(response);
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @GetMapping("/{shareCode}")
    public ResponseEntity<PollResponse.Detail> getPoll(
            @PathVariable String shareCode,
            @CookieValue(name = "participantToken", required = false) String exstingToken,
            @CookieValue(name = "hostToken", required = false) String hostToken) {

        PollResponse.Detail response = pollService.getPoll(shareCode, exstingToken, hostToken);

        if (exstingToken != null) {
            return ResponseEntity.ok(response); // 쿠키가 있다면 바로 반환
        }

        String participantToken = UUID.randomUUID().toString();

        // 만료기간 고려하여 동적 maxAge 계산
        //long secondsUntilExpiry = ChronoUnit.SECONDS.between(LocalDateTime.now(), response.expiresAt());

        ResponseCookie cookie = ResponseCookie.from("participantToken", participantToken)
                .httpOnly(true)
                .path("/")
                // TODO: 만료 투표 조회 시 별도 응답 처리 검토
                // Duration.ofSeconds(-1)을 넘기면 예외가 발생하므로 0으로 방어
                //.maxAge(Duration.ofSeconds(Math.max(secondsUntilExpiry, 0)))
                // 투표 수명과 무관하게 고정 - 단명 투표 먼저 방문 시 쿠키가 일찍 만료되어
                // 재발급 토큰으로 장수 투표에 중복 투표 가능한 구조적 허점 방지
                .maxAge(ChronoUnit.YEARS.getDuration().getSeconds() * 1)
                // URL 공유 기반 서비스 특성상 외부 링크(카카오톡 등) GET 허용 필요
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PatchMapping("/{shareCode}/close")
    public ResponseEntity<Void> closePoll(
            @PathVariable String shareCode,
            @CookieValue(name = "hostToken") String hostToken) {
        pollService.closePoll(shareCode, hostToken);
        return ResponseEntity.noContent().build();
    }
}
