package com.instance.vote.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception", e);

        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
    }

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException", e);

        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
    }

    // @CookieValue(required = true) -> 쿠기가 없을 경우
    @ExceptionHandler(MissingRequestCookieException.class)
    protected ResponseEntity<ErrorResponse> handleMissingCookie(MissingRequestCookieException e) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", "필수 쿠키가 없습니다: " + e.getCookieName()));
    }

    // @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", message));
    }

    public record ErrorResponse(
        String code,
        String message
    ) {}
}
