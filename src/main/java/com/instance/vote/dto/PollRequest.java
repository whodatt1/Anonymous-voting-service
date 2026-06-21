package com.instance.vote.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PollRequest {

    public record Create (
            String title,
            List<String> options,
            LocalDateTime expiresAt
    ) {}
}
