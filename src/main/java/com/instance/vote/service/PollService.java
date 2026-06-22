package com.instance.vote.service;

import com.instance.vote.domain.Poll;
import com.instance.vote.domain.VoteOption;
import com.instance.vote.dto.PollRequest;
import com.instance.vote.dto.PollResponse;
import com.instance.vote.exception.BusinessException;
import com.instance.vote.exception.ErrorCode;
import com.instance.vote.repository.PollRepository;
import com.instance.vote.repository.VoteOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@Transactional
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final VoteOptionRepository voteOptionRepository;

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
                poll.getHostToken()
        );
    }

    @Transactional(readOnly = true)
    public PollResponse.Detail getPoll(String shareCode) {
        Poll poll = pollRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLL_NOT_FOUND));

        List<PollResponse.OptionDetail> options = poll.getOptions().stream()
                .map(vo -> new PollResponse.OptionDetail(vo.getId(), vo.getContent(), 0L))
                .toList();

        return new PollResponse.Detail(
                poll.getShareCode(),
                poll.getTitle(),
                poll.getStatus().name(),
                poll.getExpiresAt(),
                options
        );
    }

    public void closePoll(String shareCode, String hostToken) {

    }
}
