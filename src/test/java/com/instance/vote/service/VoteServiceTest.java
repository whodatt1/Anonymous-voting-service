package com.instance.vote.service;

import com.instance.vote.domain.Poll;
import com.instance.vote.domain.VoteOption;
import com.instance.vote.dto.VoteRequest;
import com.instance.vote.event.VoteCastEvent;
import com.instance.vote.exception.BusinessException;
import com.instance.vote.exception.ErrorCode;
import com.instance.vote.port.out.DuplicateVoteCheckPort;
import com.instance.vote.port.out.VoteCountPort;
import com.instance.vote.repository.PollRepository;
import com.instance.vote.repository.VoteOptionRepository;
import com.instance.vote.repository.VoteRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class VoteServiceTest {

    @Mock
    PollRepository pollRepository;

    @Mock
    VoteOptionRepository voteOptionRepository;

    @Mock
    ApplicationEventPublisher eventPublisher; // SSE 구현하여 추가

    @Mock
    VoteRecordRepository voteRecordRepository;

    // 헥사고날 아키텍처 패턴으로 바꾸며 주석처리
    //@Mock
    //StringRedisTemplate stringRedisTemplate;

    // opsForValue() 시 ValueOperation을 리턴 StringRedisTemplate과 체이닝을 위해
    //@Mock
    //ValueOperations<String, String> valueOperations;

    // Port
    @Mock
    DuplicateVoteCheckPort duplicateVoteCheckPort;

    @Mock
    VoteCountPort voteCountPort;

    @InjectMocks
    VoteService voteService;

    private Poll openPoll;
    private VoteOption voteOption;

    @BeforeEach // 각 테스트 메서드 실행 전 실행
    void setUp() {
        openPoll = Poll.create("테스트 투표", "dummyCode", "dummyToken",
                LocalDateTime.now().plusDays(1));
        // poll.getId()는 실제로 DB가 AUTO_INCREMENT로 채워주는 값
        // 단위테스트는 DB가 없이 실행되어 null이면 검증의 무의미
        // private 필드에 값을 강제로 주입
        ReflectionTestUtils.setField(openPoll, "id", 1L);

        voteOption = VoteOption.create(openPoll, "옵션1", 1);
        ReflectionTestUtils.setField(voteOption, "id", 10L);
    }

    @Test
    void castVote_존재하지않는shareCode_예외발생() {
        given(pollRepository.findByShareCode(anyString()))
                .willReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> voteService.castVote("dummyCode", new VoteRequest.Cast(1L, "dummyToken")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_NOT_FOUND);
    }

    // DDD(Rich Entity) 전환으로 PollTest에서 검증 — 서비스 레벨 중복 제거
//    @Test
//    void castVote_이미종료된투표_예외발생() {
//        openPoll.close();
//
//        given(pollRepository.findByShareCode(anyString()))
//                .willReturn(Optional.of(openPoll));
//
//        BusinessException ex = assertThrows(BusinessException.class,
//                () -> voteService.castVote("dummyCode", new VoteRequest.Cast(1L, "dummyToken")));
//
//        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_ALREADY_CLOSED);
//    }
//
//    @Test
//    void castVote_만료된투표_예외발생() {
//        ReflectionTestUtils.setField(openPoll, "expiresAt", LocalDateTime.now().minusSeconds(1));
//
//        given(pollRepository.findByShareCode(anyString()))
//                .willReturn(Optional.of(openPoll));
//
//        BusinessException ex = assertThrows(BusinessException.class,
//                () -> voteService.castVote("dummyCode", new VoteRequest.Cast(1L, "dummyToken")));
//
//        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.POLL_EXPIRED);
//    }

    @Test
    void castVote_존재하지않는옵션_예외발생() {
        given(pollRepository.findByShareCode(anyString()))
                .willReturn(Optional.of(openPoll));

        given(voteOptionRepository.findById(any()))
                .willReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> voteService.castVote("dummyCode", new VoteRequest.Cast(1L, "dummyToken")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OPTION_NOT_FOUND);
    }

    // DDD(Rich Entity) 전환으로 VoteOptionTest에서 검증 — 서비스 레벨 중복 제거
//    @Test
//    void castVote_다른투표의옵션_예외발생() {
//        Poll otherPoll = Poll.create("다른 투표", "otherCode", "otherToken",
//                LocalDateTime.now().plusDays(1));
//        ReflectionTestUtils.setField(otherPoll, "id", 2L);
//
//        VoteOption otherOption = VoteOption.create(otherPoll, "다른 옵션", 1);
//        ReflectionTestUtils.setField(otherOption, "id", 99L);
//
//        given(pollRepository.findByShareCode(anyString()))
//                .willReturn(Optional.of(openPoll));
//
//        given(voteOptionRepository.findById(any()))
//                .willReturn(Optional.of(otherOption));
//
//        BusinessException ex = assertThrows(BusinessException.class,
//                () -> voteService.castVote("dummyCode", new VoteRequest.Cast(99L, "dummyToken")));
//
//        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OPTION_NOT_IN_POLL);
//    }

    @Test
    void castVote_중복투표_레디스차단_예외발생() {
        given(pollRepository.findByShareCode(anyString()))
                .willReturn(Optional.of(openPoll));

        given(voteOptionRepository.findById(any()))
                .willReturn(Optional.of(voteOption));

        // 헥사고날 아키텍처 패턴으로 바꾸며 주석처리
        // opsForValue()가 valueOperations를 반환하도록 먼저 설정
        //given(stringRedisTemplate.opsForValue())
        //        .willReturn(valueOperations);

        // setIfAbsent가 false를 반환 -> 이미 투표한 사람
        //given(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
        //        .willReturn(false);

        given(duplicateVoteCheckPort.isFirstVote(anyLong(), anyString(), anyLong())).willReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> voteService.castVote("dummyCode", new VoteRequest.Cast(10L, "dummyToken")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_VOTE);
    }

    @Test
    void castVote_중복투표_DB차단_예외발생() {
        given(pollRepository.findByShareCode(anyString()))
                .willReturn(Optional.of(openPoll));

        given(voteOptionRepository.findById(any()))
                .willReturn(Optional.of(voteOption));

        // 헥사고날 아키텍처 패턴으로 바꾸며 주석처리
        //given(stringRedisTemplate.opsForValue())
        //        .willReturn(valueOperations);

        //given(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
        //        .willReturn(true);

        given(duplicateVoteCheckPort.isFirstVote(anyLong(), anyString(), anyLong())).willReturn(true);

        given(voteRecordRepository.save(any()))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> voteService.castVote("dummyCode", new VoteRequest.Cast(10L, "dummyToken")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_VOTE);
    }

    @Test
    void castVote_정상케이스_성공() {
        given(pollRepository.findByShareCode(anyString()))
                .willReturn(Optional.of(openPoll));

        given(voteOptionRepository.findById(any()))
                .willReturn(Optional.of(voteOption));

        // 헥사고날 아키텍처 패턴으로 바꾸며 주석처리
        //given(stringRedisTemplate.opsForValue())
        //        .willReturn(valueOperations);

        //given(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
        //        .willReturn(true);

        given(duplicateVoteCheckPort.isFirstVote(anyLong(), anyString(), anyLong())).willReturn(true);

        assertDoesNotThrow(
                () -> voteService.castVote("dummyCode", new VoteRequest.Cast(10L, "dummyToken")));

        verify(voteRecordRepository).save(any());
        verify(voteCountPort).increment(anyLong(), anyLong());
        verify(eventPublisher).publishEvent(any(VoteCastEvent.class));
    }

}
