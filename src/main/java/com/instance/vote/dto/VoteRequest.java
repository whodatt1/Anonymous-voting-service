package com.instance.vote.dto;

public class VoteRequest {

    public record Cast(
            Long optionId,
            String participantToken
    ) {}
}
