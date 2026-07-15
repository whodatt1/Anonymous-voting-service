package com.instance.vote.scheduler;

import com.instance.vote.domain.Poll;
import com.instance.vote.repository.PollRepository;
import com.instance.vote.repository.VoteOptionRepository;
import com.instance.vote.repository.VoteRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PollCleanupScheduler {

    private final PollRepository pollRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteRecordRepository voteRecordRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredPolls() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<Poll> targets = pollRepository.findAllClosedBefore(threshold);

        if (targets.isEmpty()) { return; }

        List<Long> pollIds = targets.stream().map(Poll::getId).toList();

        voteRecordRepository.deleteByPollIdIn(pollIds);
        voteOptionRepository.deleteByPollIdIn(pollIds);
        pollRepository.deleteAll(targets);

        for (Long pollId : pollIds) {
            Set<String> keys = stringRedisTemplate.keys("vote:count:" + pollId + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        }

        log.info("[PollCleanup] {}개 투표 삭제 완료 (기준: 종료 후 30일)", targets.size());
    }
}
