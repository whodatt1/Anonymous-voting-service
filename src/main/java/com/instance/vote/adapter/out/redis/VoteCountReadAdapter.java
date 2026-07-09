package com.instance.vote.adapter.out.redis;

import com.instance.vote.port.out.VoteCountReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VoteCountReadAdapter implements VoteCountReadPort {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Map<Long, Long> getCounts(Long pollId, List<Long> optionIds) {
        return optionIds.stream()
                .collect(
                        Collectors.toMap(
                            optionId -> optionId,
                                optionId -> {
                                    String key = "vote:count:" + pollId + ":" + optionId;
                                    String value = stringRedisTemplate.opsForValue().get(key);
                                    return value != null ? Long.parseLong(value) : 0L;
                                }
                        )
                );
    }
}
