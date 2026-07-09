package com.instance.vote.adapter.out.redis;

import com.instance.vote.port.out.VoteCountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisVoteCountAdapter implements VoteCountPort {

    // 문자열 키/값만 쓰는 경우 StringRedisTemplate을 주로 사용
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void increment(Long pollId, Long optionId) {
        // Redis INCR - 집계 카운트
        String countKey = "vote:count:" + pollId + ":" + optionId;
        stringRedisTemplate.opsForValue().increment(countKey);
    }
}
