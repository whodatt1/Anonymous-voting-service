package com.instance.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "vote_record",
       uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_vote_record_poll_participant",
                columnNames = {
                        "poll_id",
                        "participant_token"
                }
        )
       })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB 내부 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll; // 소속 투표

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private VoteOption option; // 선택한 선택지

    @Column(name = "participant_token", nullable = false)
    private String participantToken; // 참여자 UUID

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 투표 시각

    public static VoteRecord create(Poll poll, VoteOption option,
                                    String participantToken) {
        VoteRecord voteRecord = new VoteRecord();
        voteRecord.poll = poll;
        voteRecord.option = option;
        voteRecord.participantToken = participantToken;
        return voteRecord;
    }
}
