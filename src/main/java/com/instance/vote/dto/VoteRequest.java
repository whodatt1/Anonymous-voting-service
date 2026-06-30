package com.instance.vote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VoteRequest {

    // 클라이언트에서 전달받는 Body
    public record Body(
            @NotNull Long optionId
    ) {}

    // Controller가 조립하여 Service로 전달
    public record Cast(
            Long optionId,
            String participantToken
    ) {}
}
