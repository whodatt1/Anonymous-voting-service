package com.instance.vote.service;

import com.instance.vote.domain.Poll;
import com.instance.vote.domain.PollStatus;
import com.instance.vote.domain.VoteOption;
import com.instance.vote.dto.PollRequest;
import com.instance.vote.dto.PollResponse;
import com.instance.vote.event.SseEmitterManager;
import com.instance.vote.exception.BusinessException;
import com.instance.vote.exception.ErrorCode;
import com.instance.vote.repository.PollRepository;
import com.instance.vote.repository.VoteOptionRepository;
import com.instance.vote.repository.VoteRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PollServiceTest {

    @Mock
    StringRedisTemplate stringRedisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @Mock
    VoteRecordRepository voteRecordRepository;

    @Mock
    PollRepository pollRepository; // 가짜 Repo

    @Mock
    VoteOptionRepository voteOptionRepository;

    @Mock
    SseEmitterManager sseEmitterManager;

    @InjectMocks
    PollService pollService; // 위 Mock들이 주입된 진짜 Service

    @Test
    void validateSseConnection_존재하지않는투표_예외발생() {
        given(pollRepository.findWithOptionByShareCode(anyString()))
                .willReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.validateSseConnection("dummyCode", "dummyToken"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_NOT_FOUND);
    }

    @Test
    void validateSseConnection_호스트토큰불일치_예외발생() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "correctToken", LocalDateTime.now().plusDays(1));

        given(pollRepository.findWithOptionByShareCode(anyString()))
                .willReturn(Optional.of(poll));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.validateSseConnection("dummyCode", "wrongToken"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZE_HOST);
    }

    @Test
    void validateSseConnection_이미닫힌투표_예외발생() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken", LocalDateTime.now().plusDays(1));
        poll.close();

        given(pollRepository.findWithOptionByShareCode(anyString()))
                .willReturn(Optional.of(poll));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.validateSseConnection("dummyCode", "dummyToken"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_ALREADY_CLOSED);
    }

    @Test
    void validateSseConnection_만료된투표_예외발생() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken", LocalDateTime.now().minusDays(1));

        given(pollRepository.findWithOptionByShareCode(anyString()))
                .willReturn(Optional.of(poll));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.validateSseConnection("dummyCode", "dummyToken"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_EXPIRED);
    }

    @Test
    void validateSseConnection_정상케이스_성공() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken",
                LocalDateTime.now().plusDays(1));

        given(pollRepository.findWithOptionByShareCode(anyString()))
                .willReturn(Optional.of(poll));

        PollResponse.Detail response = pollService.validateSseConnection("dummyCode", "dummyToken");

        assertThat(response.shareCode()).isEqualTo(poll.getShareCode());
        assertThat(response.title()).isEqualTo(poll.getTitle());
        assertThat(response.status()).isEqualTo(poll.getStatus().name());
    }

    @Test
    void getPoll_존재하지않는투표_예외발생() {
        given(pollRepository.findWithOptionByShareCode(anyString()))
                .willReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pollService.getPoll("dummyCode", null));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_NOT_FOUND);
    }

    @Test
    void getPoll_Redis장애시_DB폴백으로집계반환() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken",
                LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(poll, "id", 1L);

        VoteOption option = VoteOption.create(poll, "옵션1", 1);
        ReflectionTestUtils.setField(option, "id", 10L);
        // JPA 없이는 poll.getOptions()가 비어있으므로 강제 주입
        ReflectionTestUtils.setField(poll, "options", List.of(option));

        given(pollRepository.findWithOptionByShareCode(anyString()))
                .willReturn(Optional.of(poll));

        // Redis가 예외를 던지도록 설정 -> DB 폴백 트리거
        given(stringRedisTemplate.opsForValue())
                .willThrow(new RuntimeException("Redis 연결 실패"));

        given(voteRecordRepository.countByPollIdGroupByOption(anyLong()))
                .willReturn(List.of(new VoteRecordRepository.OptionCount(10L, 5L)));

        PollResponse.Detail response = pollService.getPoll("dummyCode", null);

        assertThat(response.options().get(0).count()).isEqualTo(5L);
    }

    @Test
    void getPoll_정상케이스_성공() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken", LocalDateTime.now().plusDays(1));

        given(pollRepository.findWithOptionByShareCode(anyString()))
                .willReturn(Optional.of(poll));

        PollResponse.Detail response = pollService.getPoll("dummyCode", null);

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
