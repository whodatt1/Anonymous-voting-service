package com.instance.vote.adapter.out.redis;

import com.instance.vote.port.out.DuplicateVoteCheckPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisDuplicateVoteCheckAdapter implements DuplicateVoteCheckPort {

    // 문자열 키/값만 쓰는 경우 StringRedisTemplate을 주로 사용
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean isFirstVote(Long pollId, String participantToken, long ttlSeconds) {

        // Redis SEXNX(Set if Not eXists) - 중복 투표 1차 방어
        String dupKey = "vote:dup:" + pollId + ":" + participantToken;
        Boolean isNew = stringRedisTemplate.opsForValue() // String 타입 키/값 조작
                .setIfAbsent(dupKey, "1", ttlSeconds, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(isNew); // Redis가 null 이면 NPE 발생 -> null이면 false로 안전하게 처리
    }
}
