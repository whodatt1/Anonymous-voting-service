package com.instance.vote.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

// @Configuration: 이 클래스가 Spring 빈을 등록하는 설정 클래스임을 선언
@Configuration
@RequiredArgsConstructor
public class RateLimitConfig {

    // application.yaml의 spring.data.redis.host / port 값을 Spring이 자동으로 담아주는 클래스
    // 우리가 직접 @Value로 꺼내지 않아도 getHost(), getPort()로 바로 사용 가능
    private final RedisProperties redisProperties;

    // (1) Bucket4j 전용 RedisClient 빈
    // Spring Data Redis도 내부적으로 RedisClient를 만들지만 외부에 노출하지 않는다
    // Bucket4j는 자신이 직접 다룰 수 있는 RedisClient가 필요하므로 별도로 생성
    // destroyMethod = "shutdown": 앱이 종료될 때 Spring이 자동으로 shutdown()을 호출해 연결을 정상 해제
    @Bean(destroyMethod = "shutdown")
    public RedisClient bucketRedisClient() {
        return RedisClient.create(
                // RedisURI: "어느 Redis 서버에 연결할지"를 표현하는 객체
                // application.yaml의 환경변수 값을 그대로 읽어 같은 Redis 서버에 연결
                RedisURI.builder()
                        .withHost(redisProperties.getHost())
                        .withPort(redisProperties.getPort())
                        .build()
        );
    }

    // (2) LettuceBasedProxyManager 빈
    // ProxyManager: "키를 주면 해당 키의 버킷을 Redis에서 꺼내거나 없으면 새로 만들어주는" Bucket4j의 브리지
    // RateLimitAspect에서 이 빈을 주입받아 버킷을 조회한다
    @Bean
    public LettuceBasedProxyManager<byte[]> rateLimitProxyManager(RedisClient bucketRedisClient) {
        // ByteArrayCodec.INSTANCE: Redis와 byte[] 형식으로 통신하도록 지정
        // Bucket4j가 내부적으로 Lua 스크립트로 원자적 연산을 수행할 때 byte[] 형식을 사용하기 때문
        StatefulRedisConnection<byte[], byte[]> connection =
                bucketRedisClient.connect(ByteArrayCodec.INSTANCE);

        return LettuceBasedProxyManager.builderFor(connection)
                .withClientSideConfig(
                        // ClientSideConfig: 클라이언트 측 동작 설정을 묶는 객체
                        // getDefault()로 기본값을 가져온 뒤 만료 전략만 덮어씌움
                        ClientSideConfig.getDefault().withExpirationAfterWriteStrategy(
                                // 버킷 상태를 저장한 Redis 키의 TTL 전략
                                // basedOnTimeForRefillingBucketUpToMax: 버킷이 완전히 리필되는 데 걸리는 시간만큼 TTL 부여
                                // 즉 마지막 요청 이후 윈도우(10초)가 지나면 Redis 키가 자동 삭제되어 메모리 누수를 방지
                                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10))
                        )
                )
                .build();
    }
}
