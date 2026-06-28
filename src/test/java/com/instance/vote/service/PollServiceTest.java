package com.instance.vote.service;

import com.instance.vote.domain.Poll;
import com.instance.vote.domain.PollStatus;
import com.instance.vote.dto.PollRequest;
import com.instance.vote.dto.PollResponse;
import com.instance.vote.exception.BusinessException;
import com.instance.vote.exception.ErrorCode;
import com.instance.vote.repository.PollRepository;
import com.instance.vote.repository.VoteOptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PollServiceTest {

    @Mock
    PollRepository pollRepository; // 가짜 Repo

    @Mock
    VoteOptionRepository voteOptionRepository;

    @InjectMocks
    PollService pollService; // 위 Mock들이 주입된 진짜 Service

    @Test
    void getPoll_존재하지않는투표_예외발생() {
        given(pollRepository.findWithOptionByShareCode(anyString()))
                .willReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.getPoll("dummyCode"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_NOT_FOUND);
    }

    @Test
    void getPoll_정상케이스_성공() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken", LocalDateTime.now().plusDays(1));

        given(pollRepository.findWithOptionByShareCode(anyString()))
                .willReturn(Optional.of(poll));

        PollResponse.Detail response = pollService.getPoll("dummyCode");

        assertThat(response.shareCode()).isEqualTo(poll.getShareCode());
        assertThat(response.title()).isEqualTo(poll.getTitle());
        assertThat(response.status()).isEqualTo(poll.getStatus().name());
    }

    @Test
    void createPoll_정상케이스_성공() {
        PollRequest.Create request = new PollRequest.Create(
                "테스트 투표",
                List.of("옵션1", "옵션2"),
                LocalDateTime.now().plusDays(1)
        );

        PollResponse.Create result = pollService.createPoll(request);

        assertThat(result.shareCode()).isNotNull();
        assertThat(result.hostToken()).isNotNull();
        verify(pollRepository).save(any(Poll.class));
    }

    @Test
    void closePoll_존재하지않는shareCode_예외발생() {
        given(pollRepository.findByShareCode(anyString()))
                .willReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.closePoll("dummyCode", "dummyToken"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_NOT_FOUND);
    }

    @Test
    void closePoll_호스트토큰불일치_예외발생() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "correctToken", LocalDateTime.now().plusDays(1));

        given(pollRepository.findByShareCode(anyString()))
                .willReturn(Optional.of(poll));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.closePoll("dummyCode", "wrongToken"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZE_HOST);
    }

    @Test
    void closePoll_이미닫힌투표_예외발생() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken", LocalDateTime.now().plusDays(1));
        poll.close();

        given(pollRepository.findByShareCode(anyString()))
                .willReturn(Optional.of(poll));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.closePoll("dummyCode", "dummyToken"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_ALREADY_CLOSED);
    }

    @Test
    void closePoll_정상케이스_성공() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken", LocalDateTime.now().plusDays(1));

        given(pollRepository.findByShareCode(anyString()))
                .willReturn(Optional.of(poll));

        assertDoesNotThrow(() -> pollService.closePoll("dummyCode", "dummyToken"));

        assertThat(poll.getStatus()).isEqualTo(PollStatus.CLOSED);
    }
}
