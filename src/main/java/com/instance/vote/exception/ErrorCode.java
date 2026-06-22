package com.instance.vote.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "POLL_001", "투표를 찾을 수 없습니다."),
    UNAUTHORIZE_HOST(HttpStatus.FORBIDDEN, "POLL_002", "투표 관리 권한이 없습니다."),
    POLL_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "POLL_003", "이미 종료된 투표입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
