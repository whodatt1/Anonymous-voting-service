package com.instance.vote.domain;

import com.instance.vote.exception.BusinessException;
import com.instance.vote.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class VoteOptionTest {

    private VoteOption voteOption;

    @BeforeEach
    void setUp() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken", LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(poll, "id", 1L);
        voteOption = VoteOption.create(poll, "옵션1", 1);
    }

    @Test
    void 다른_투표의_옵션은_검증_실패한다() {

        // 다른 투표 id -> 예외
        assertThatThrownBy(() -> voteOption.validateBelongsToPoll(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPTION_NOT_IN_POLL);

    }

    @Test
    void 같은_투표의_옵션은_검증을_통과한다() {

        assertDoesNotThrow(() -> voteOption.validateBelongsToPoll(1L));

    }
}
