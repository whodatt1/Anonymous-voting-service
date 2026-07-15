package com.instance.vote.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "POLL_001", "투표를 찾을 수 없습니다."),
    UNAUTHORIZE_HOST(HttpStatus.FORBIDDEN, "POLL_002", "투표 관리 권한이 없습니다."),
    POLL_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "POLL_003", "이미 종료된 투표입니다."),
    POLL_EXPIRED(HttpStatus.BAD_REQUEST, "POLL_004", "만료 시각이 지난 투표입니다."),
    OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "OPTION_001", "옵션을 찾을 수 없습니다."),
    OPTION_NOT_IN_POLL(HttpStatus.BAD_REQUEST, "OPTION_002", "해당 투표 소속의 옵션이 아닙니다."),
    DUPLICATE_VOTE(HttpStatus.CONFLICT, "VOTE_001", "이미 해당 투표에 참여하였습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "VOTE_429", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    POLL_EXPIRY_EXCEEDS_LIMIT(HttpStatus.BAD_REQUEST, "POLL_005", "투표 만료 기간은 최대 7일까지 설정 가능합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
