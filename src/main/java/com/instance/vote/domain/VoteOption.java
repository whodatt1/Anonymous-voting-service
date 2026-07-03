package com.instance.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "vote_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB 내부 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll; // 소속 투표

    @Column(name = "content", nullable = false)
    private String content; // 선택지 내용

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder; // 화면 표시 순서 보장용

    public static VoteOption create(Poll poll, String content, int displayOrder) {
        VoteOption option = new VoteOption();
        option.poll = poll;
        option.content = content;
        option.displayOrder = displayOrder;
        return option;
    }
}
