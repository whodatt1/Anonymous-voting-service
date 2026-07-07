package com.instance.vote.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    // LettuceBasedProxyManager 빈
    // ProxyManager: "키를 주면 해당 키의 버킷을 Redis에서 꺼내거나 없으면 새로 만들어주는" Bucket4j의 브리지
    // RateLimitAspect에서 이 빈을 주입받아 버킷을 조회한다
    @Bean
    public LettuceBasedProxyManager<byte[]> rateLimitProxyManager(
            LettuceConnectionFactory lettuceConnectionFactory) {

        // Spring이 이미 @ServiceConnection / application.yaml 기반으로 올바르게 구성한
        // LettuceConnectionFactory에서 내부 RedisClient를 꺼낸다
        // → 별도 RedisClient 빈을 만들면 테스트 환경의 @ServiceConnection이 RedisProperties를 덮어쓰지 않아
        //   CI에서 localhost:6379 연결 실패가 발생하므로 이 방식으로 통일
        // → getNativeConnection()은 RedisAsyncCommandsImpl을 반환하므로 사용 불가;
        //   getRequiredNativeClient()로 RedisClient 자체를 꺼내야 한다
        RedisClient redisClient = (RedisClient) lettuceConnectionFactory.getRequiredNativeClient();

        // Bucket4j는 Lua 스크립트로 원자적 연산을 수행하므로 byte[] 형식의 연결이 필요하다
        // 앱 시작 시 한 번, ByteArrayCodec 전용 연결을 열어 ProxyManager에 고정
        // Lettuce는 단일 연결 + 멀티플렉싱 방식이므로 이 연결 하나로 모든 요청을 처리
        StatefulRedisConnection<byte[], byte[]> nativeConnection =
                redisClient.connect(ByteArrayCodec.INSTANCE);

        return LettuceBasedProxyManager.builderFor(nativeConnection)
                .withClientSideConfig(
                        // ClientSideConfig: 클라이언트 측 동작 설정을 묶는 객체
                        // getDefault()로 기본값을 가져온 뒤 TTL 전략만 덮어씌움
                        ClientSideConfig.getDefault().withExpirationAfterWriteStrategy(
                                // 버킷이 완전히 리필되는 데 걸리는 시간 + 10초 버퍼를 TTL로 설정
                                // 버퍼 10초: 앱 서버·Redis 서버 간 클럭 스큐로 키가 조기 삭제되어
                                // Rate Limiting이 우회되는 것을 방지
                                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10))
                        )
                )
                .build();
    }
}
