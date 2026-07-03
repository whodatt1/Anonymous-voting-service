package com.instance.vote.repository;

import com.instance.vote.domain.Poll;
import com.instance.vote.domain.VoteOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

    List<VoteOption> findByPollOrderByDisplayOrder(Poll poll);
}
