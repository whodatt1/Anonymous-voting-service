package com.instance.vote.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public class PollRequest {

    public record Create (
            @NotBlank @Size(max = 100) String title,
            @NotEmpty @Size(min = 2, max = 5) List<@NotBlank @Size(max = 50) String> options,
            // @Future -> 현재보다 미래 조건 강제
            @NotNull @Future LocalDateTime expiresAt
    ) {}
}
