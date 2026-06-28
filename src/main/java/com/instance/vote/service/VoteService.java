package com.instance.vote.service;

import com.instance.vote.domain.Poll;
import com.instance.vote.domain.PollStatus;
import com.instance.vote.domain.VoteOption;
import com.instance.vote.domain.VoteRecord;
import com.instance.vote.dto.VoteRequest;
import com.instance.vote.exception.BusinessException;
import com.instance.vote.exception.ErrorCode;
import com.instance.vote.repository.PollRepository;
import com.instance.vote.repository.VoteOptionRepository;
import com.instance.vote.repository.VoteRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@RequiredArgsConstructor
public class VoteService {

    private final PollRepository pollRepository;
    private final VoteRecordRepository voteRecordRepository;
    // 문자열 키/값만 쓰는 경우 StringRedisTemplate을 주로 사용
    private final StringRedisTemplate stringRedisTemplate;
    private final VoteOptionRepository voteOptionRepository;

    public void castVote(String shareCode, VoteRequest.Cast request) {
        // Poll 조회 + 투표 가능 여부 검증
        Poll poll = pollRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLL_NOT_FOUND));

        // 종료된 투표인지 검사
        if (poll.getStatus() == PollStatus.CLOSED) {
            throw new BusinessException(ErrorCode.POLL_ALREADY_CLOSED);
        }

        // 만료 시간이 지났는지 검사
        if (poll.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.POLL_EXPIRED);
        }

        // VoteOption 조회 + Poll 소속 확인
        VoteOption voteOption = voteOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));

        if (!Objects.equals(poll.getId(), voteOption.getPoll().getId())) {
            throw new BusinessException(ErrorCode.OPTION_NOT_IN_POLL);
        }

        // Redis SEXNX(Set if Not eXists) - 중복 투표 1차 방어
        String dupKey = "vote:dup:" + poll.getId() + ":" + request.participantToken();
        long ttl = Duration.between(LocalDateTime.now(), poll.getExpiresAt()).getSeconds();
        Boolean isNew = stringRedisTemplate.opsForValue() // String 타입 키/값 조작
                .setIfAbsent(dupKey, "1", ttl, TimeUnit.SECONDS);

        if (!isNew) {
            throw new BusinessException(ErrorCode.DUPLICATE_VOTE);
        }

        try {
            voteRecordRepository.save(
                    VoteRecord.create(poll, voteOption, request.participantToken())
            );
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_VOTE);
        }


        // Redis INCR - 집계 카운트
        String countKey = "vote:count:" + poll.getId() + ":" + request.optionId();
        stringRedisTemplate.opsForValue().increment(countKey);
    }
}
