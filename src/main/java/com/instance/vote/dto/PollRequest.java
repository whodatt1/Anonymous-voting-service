package com.instance.vote.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public class PollRequest {

    public record Create (
            @NotBlank String title,
            @NotEmpty @Size(min = 2) List<String> options,
            // @Future -> 현재보다 미래 조건 강제
            @NotNull @Future LocalDateTime expiresAt
    ) {}
}
