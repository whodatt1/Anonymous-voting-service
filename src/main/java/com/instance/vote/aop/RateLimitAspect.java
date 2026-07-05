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

        // 2. participantToken 쿠키 추출 (없다면 null)
        String token = extractCookie(request, "participantToken");

        // 3. 토큰이 없을 경우 첫 방문 -> Rate Limiting 없이 통과
        if (token == null) {
            return joinPoint.proceed();
        }

        // 4. Redis 키 구성
        String bucketKey = "rate:" + token + ":" + joinPoint.getSignature().getName();

        // 5. 버킷 설정 - @RateLimit 어노테이션 값 적용
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rateLimit.limit())
                        // 30개 60초로 가정 시 30 % 60 = 0.5개/초 속도로 채워짐
                        // intervally는 고정 윈도우 방식 59초에 30개, 1초뒤 30개 총 60개가 순간 통과 가능해지므로 해당 방식 채택
                        .refillGreedy(rateLimit.limit(), Duration.ofSeconds(rateLimit.windowSeconds()))
                        .build())
                .build();

        // 6. ProxyManager로 버킷 조회 (해당 키가 Redis에 없으면 위 설정으로 새로 생성)
        Bucket bucket = rateLimitProxyManager.builder()
                .build(bucketKey.getBytes(), () -> configuration);

        // 7. 토큰 소비 시도
        if (!bucket.tryConsume(1)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        return joinPoint.proceed();
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

}
