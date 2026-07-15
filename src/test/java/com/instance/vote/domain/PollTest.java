package com.instance.vote.domain;

import com.instance.vote.exception.BusinessException;
import com.instance.vote.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class PollTest {

    @Test
    void 마감된_투표에는_투표할_수_없다() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken", LocalDateTime.now().plusDays(1));
        poll.close();

        assertThatThrownBy(() -> poll.validateVotable())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POLL_ALREADY_CLOSED);
    }

    @Test
    void 만료된_투표에는_투표할_수_없다() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "dummyToken", LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> poll.validateVotable())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POLL_EXPIRED);
    }

    @Test
    void 잘못된_호스트_토큰으로는_접근할_수_없다() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "correctToken", LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> poll.validateHost("wrongToken"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZE_HOST);
    }

    @Test
    void 이미_닫힌_투표는_다시_닫을_수_없다() {
        Poll poll = Poll.create("테스트 투표", "dummyCode", "correctToken", LocalDateTime.now().plusDays(1));
        poll.close();

        assertThatThrownBy(() -> poll.validateNotClosed())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POLL_ALREADY_CLOSED);
    }

    @Test
    void 만료_기한이_7일을_초과하면_투표를_생성할_수_없다() {
        assertThatThrownBy(() ->
            Poll.create("테스트 투표", "dummyCode", "dummyToken", LocalDateTime.now().plusDays(8))
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POLL_EXPIRY_EXCEEDS_LIMIT);
    }

    @Test
    void 투표를_닫으면_closeAt이_설정된다() {
        Poll poll = Poll.create("테스트 투표",  "dummyCode", "dummyToken", LocalDateTime.now().plusDays(1));
        poll.close();

        assertThat(poll.getClosedAt()).isNotNull();
        assertThat(poll.getClosedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
}
