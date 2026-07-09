package com.instance.vote.service;

import com.instance.vote.domain.Poll;
import com.instance.vote.domain.PollStatus;
import com.instance.vote.domain.VoteOption;
import com.instance.vote.dto.PollRequest;
import com.instance.vote.dto.PollResponse;
import com.instance.vote.event.SseEmitterManager;
import com.instance.vote.exception.BusinessException;
import com.instance.vote.exception.ErrorCode;
import com.instance.vote.port.out.VoteCountReadPort;
import com.instance.vote.repository.PollRepository;
import com.instance.vote.repository.VoteOptionRepository;
import com.instance.vote.repository.VoteRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PollService {

    private final SseEmitterManager sseEmitterManager;
    private final PollRepository pollRepository;
    private final VoteOptionRepository voteOptionRepository;
    // 포트 호출로 변경
    //private final StringRedisTemplate stringRedisTemplate;
    private final VoteRecordRepository voteRecordRepository;

    // Port
    private final VoteCountReadPort voteCountReadPort;

    public PollResponse.Create createPoll(PollRequest.Create request) {

        String shareCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String hostToken = UUID.randomUUID().toString();

        Poll poll = Poll.create(request.title(), shareCode, hostToken, request.expiresAt());
        pollRepository.save(poll);

        List<VoteOption> optionList = IntStream.range(0, request.options().size())
                .mapToObj(i -> VoteOption.create(poll, request.options().get(i), i + 1))
                .toList();

        voteOptionRepository.saveAll(optionList);

        return new PollResponse.Create(
                poll.getShareCode(),
                poll.getHostToken(),
                poll.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public PollResponse.Detail getHostPoll(String shareCode, String hostToken) {
        Poll poll = pollRepository.findWithOptionByShareCode(shareCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLL_NOT_FOUND));

        // 호스트 토큰 검증
        if (!hostToken.equals(poll.getHostToken())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZE_HOST);
        }

        return toDetail(poll, null, hostToken);
    }

    @Transactional(readOnly = true)
    public PollResponse.Detail getPoll(String shareCode, String participantToken, String hostToken) {
        Poll poll = pollRepository.findWithOptionByShareCode(shareCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLL_NOT_FOUND));

        return toDetail(poll, participantToken, hostToken);
    }

    public void closePoll(String shareCode, String hostToken) {
        Poll poll = pollRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLL_NOT_FOUND));

        // 호스트 토큰 검증
        if (!hostToken.equals(poll.getHostToken())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZE_HOST);
        }

        // 닫혔는지 여부
        if (poll.getStatus() == PollStatus.CLOSED) {
            throw new BusinessException(ErrorCode.POLL_ALREADY_CLOSED);
        }

        poll.close();
        sseEmitterManager.completeAll(poll.getId());
    }

    // Sse 연결 검증용 메서드
    @Transactional(readOnly = true)
    public PollResponse.Detail validateSseConnection(String shareCode, String hostToken) {
        Poll poll = pollRepository.findWithOptionByShareCode(shareCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLL_NOT_FOUND));

        // 호스트 토큰 검증
        if (!hostToken.equals(poll.getHostToken())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZE_HOST);
        }

        // 닫혔는지 여부
        if (poll.getStatus() == PollStatus.CLOSED) {
            throw new BusinessException(ErrorCode.POLL_ALREADY_CLOSED);
        }

        // 만료 시간이 지났는지 검사
        if (poll.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.POLL_EXPIRED);
        }

        return toDetail(poll, null, hostToken);
    }

    // 중복되는 코드 private 메서드로 분리
    private PollResponse.Detail toDetail(Poll poll, String participantToken, String hostToken) {
        // 람다 안에서 외부 변수를 쓰려면 해당 변수가 한번만 할당되어야 함 별도 private 메서드로 추출하여 한번만 할당
        Map<Long, Long> counts = resolveVoteCounts(poll.getId(), poll.getOptions());

        List<PollResponse.OptionDetail> options = poll.getOptions().stream()
                .map(vo -> new PollResponse.OptionDetail(vo.getId(), vo.getContent(), counts.getOrDefault(vo.getId(), 0L)))
                .toList();

        boolean hasVoted = participantToken != null &&
                voteRecordRepository.existsByPoll_IdAndParticipantToken(poll.getId(), participantToken);

        boolean isHost = hostToken != null &&
                hostToken.equals(poll.getHostToken());

        return new PollResponse.Detail(
                poll.getId(),
                poll.getShareCode(),
                poll.getTitle(),
                poll.getStatus().name(),
                poll.getExpiresAt(),
                options,
                hasVoted,
                isHost
        );
    }

    private Map<Long, Long> resolveVoteCounts(Long pollId, List<VoteOption> options) {
        try {
            // 포트 호출로 변경
            //return getCountsFromRedis(pollId, options);
            List<Long> optionIds = options.stream().map(VoteOption::getId).toList();
            return voteCountReadPort.getCounts(pollId, optionIds);
        } catch (Exception e) {
            log.warn("Redis 조회 실패, DB 폴백", e);
            return getCountsFromDb(pollId);
        }
    }

    // Redis에서 옵션별 득표 수 추출 -> 포트 호출로 변경
//    private Map<Long, Long> getCountsFromRedis(Long pollId, List<VoteOption> options) {
//        return options.stream()
//                .collect(
//                        Collectors.toMap(
//                        VoteOption::getId, // key : optionId
//                        option -> { // value : Redis에서 꺼낸 count
//                            String key = "vote:count:" + pollId + ":" + option.getId();
//                            String value = stringRedisTemplate.opsForValue().get(key);
//                            return value != null ? Long.parseLong(value) : 0L;
//                        }
//                ));
//    }

    private Map<Long, Long> getCountsFromDb(Long pollId) {
        return voteRecordRepository.countByPollIdGroupByOption(pollId).stream()
                .collect(Collectors.toMap(
                        VoteRecordRepository.OptionCount::optionId, // key : optionId
                        VoteRecordRepository.OptionCount::count
                ));
    }
}
