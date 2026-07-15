package com.instance.vote.domain;

import com.instance.vote.exception.BusinessException;
import com.instance.vote.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "poll")
// JPA가 관리할 Entity. 외부에서 new Poll()로 생성하는 것을 방지
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB 내부 식별자

    @Column(name = "share_code", nullable = false, unique = true)
    private String shareCode; // 공유 URL용 랜덤 코드. ex) votes/aB3zZ9

    @Column(name = "host_token", nullable = false)
    private String hostToken; // 생성자 UUID. 관리 URL 쿼리 파라미터와 비교해 생성자 인증에 사용

    @Column(name = "title", nullable = false)
    private String title; // 투표 제목

    @Enumerated(EnumType.STRING) // 지정 안할 시 OPEN=0, CLOSED=1 처럼 숫자로 저장
    @Column(name = "status", nullable = false)
    private PollStatus status; // 투표 상태

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // 투표 마감 시각

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 시각

    @OneToMany(mappedBy = "poll", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<VoteOption> options = new ArrayList<>();

    public static Poll create(String title, String shareCode,
                              String hostToken, LocalDateTime expiresAt) {
        // 생성 시 유효 기간 7일 미만 비즈니스 규칙 규정
        if (ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt) > 7) {
            throw new BusinessException(ErrorCode.POLL_EXPIRY_EXCEEDS_LIMIT);
        }

        Poll poll = new Poll();
        poll.title = title;
        poll.shareCode = shareCode;
        poll.hostToken = hostToken;
        poll.status = PollStatus.OPEN;
        poll.expiresAt = expiresAt;
        return poll;
    }

    // 투표 종료
    public void close() {
        this.status = PollStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    // Rich Entity
    // 투표 가능 여부
    public void validateVotable() {
        if (this.status == PollStatus.CLOSED) {
            throw new BusinessException(ErrorCode.POLL_ALREADY_CLOSED);
        }

        if (this.expiresAt.isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.POLL_EXPIRED);
        }
    }

    // 호스트 검증
    public void validateHost(String hostToken) {
        if (!this.hostToken.equals(hostToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZE_HOST);
        }
    }

    // 닫힌 투표인지 검증
    public void validateNotClosed() {
        if (this.status == PollStatus.CLOSED) {
            throw new BusinessException(ErrorCode.POLL_ALREADY_CLOSED);
        }
    }
}
