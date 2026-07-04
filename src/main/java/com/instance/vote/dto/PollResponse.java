package com.instance.vote.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

public class PollResponse {

    // 생성 응답 - hostToken은 쿠키로 발급, 바디에 미포함
    public record Create(
        String shareCode,
        @JsonIgnore String hostToken, // 직렬화에서 제외
        LocalDateTime expiresAt
    ) {}

    // 일반 조회 응답 - hostToken 미포함
    public record Detail (
        Long pollId,
        String shareCode,
        String title,
        String status,
        LocalDateTime expiresAt,
        List<OptionDetail> options,
        boolean hasVoted,
        boolean isHost
    ) {}

    public record OptionDetail(
            Long optionId,
            String content,
            long count // Redis에서 채울 집계 값
    ) {}
}
