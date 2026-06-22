package com.instance.vote.repository;

import com.instance.vote.domain.Poll;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    @EntityGraph(attributePaths = "options")
    Optional<Poll> findByShareCode(String shareCode);
}
