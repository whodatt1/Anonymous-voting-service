package com.instance.vote.aop;

import com.instance.vote.annotation.RateLimit;
import com.instance.vote.exception.BusinessException;
import com.instance.vote.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final LettuceBasedProxyManager<byte[]> rateLimitProxyManager;

    // 메서드를 완전히 감쌈, joinPoint.proceed()를 호출해야 원래 메서드 실행
    // @RateLimit 붙은 메서드를 찾고 -> 그 어노테이션 인스턴스를 rateLimit 파라미터에 바인딩
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {

        // 1. 현재 요청 객체 꺼내기
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();

        // 2. IP 추출 후 Redis 키 구성
        String clientIp = extractClientIp(request);
        String bucketKey = "rate:" + clientIp + ":" + joinPoint.getSignature().getName();

        // 4. 버킷 설정 - @RateLimit 어노테이션 값 적용
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rateLimit.limit())
                        // 30개 60초로 가정 시 30 % 60 = 0.5개/초 속도로 채워짐
                        // intervally는 고정 윈도우 방식 59초에 30개, 1초뒤 30개 총 60개가 순간 통과 가능해지므로 해당 방식 채택
                        .refillGreedy(rateLimit.limit(), Duration.ofSeconds(rateLimit.windowSeconds()))
                        .build())
                .build();

        // 5. ProxyManager로 버킷 조회 (해당 키가 Redis에 없으면 위 설정으로 새로 생성)
        Bucket bucket = rateLimitProxyManager.builder()
                .build(bucketKey.getBytes(), () -> configuration);

        // 6. 토큰 소비 시도
        if (!bucket.tryConsume(1)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        return joinPoint.proceed();
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For는 클라이언트가 직접 위조할 수 있다.
            // 예) 공격자가 "X-Forwarded-For: 가짜IP"를 보내면
            //     ALB가 실제 IP를 뒤에 붙여 "가짜IP, 진짜IP"로 만든다.
            // ALB는 항상 실제 클라이언트 IP를 맨 뒤에 추가하므로
            // 마지막 값이 ALB가 보장하는 신뢰할 수 있는 IP다.
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        // X-Forwarded-For가 없는 경우 = Spring에 TCP를 직접 맺은 상대방 IP.
        // 로컬 개발: 브라우저가 Spring에 직접 붙으므로 127.0.0.1.
        // ALB가 있으면 X-Forwarded-For가 항상 존재하므로 이 분기에 오지 않는다.
        return request.getRemoteAddr();
    }
}
