package com.instance.vote.service;

import com.instance.vote.domain.Poll;
import com.instance.vote.domain.VoteOption;
import com.instance.vote.dto.PollRequest;
import com.instance.vote.dto.PollResponse;
import com.instance.vote.repository.PollRepository;
import com.instance.vote.repository.VoteOptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public PollResponse.Detail getPoll(String shareCode) {


        return null;
    }

    public void closePoll(String shareCode, String hostToken) {

    }
}
