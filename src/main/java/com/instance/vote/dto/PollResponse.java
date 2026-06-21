package com.instance.vote.dto;

import com.instance.vote.domain.VoteOption;

import java.time.LocalDateTime;
import java.util.List;

public class PollResponse {

    // 생성 응답 - hostToken을 포함하여 한번만 노출
    public record Create(
        String shareCode,
        String hostToken
    ) {}

    // 일반 조회 응답 - hostToken 미포함
    public record Detail (
        String shareCode,
        String title,
        String status,
        LocalDateTime expiresAt,
        List<OptionDetail> options
    ) {}

    public record OptionDetail(
            Long optionId,
            String content,
            long count // Redis에서 채울 집계 값
    ) {}
}
