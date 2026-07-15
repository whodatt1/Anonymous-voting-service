package com.instance.vote.repository;

import com.instance.vote.domain.Poll;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    @EntityGraph(attributePaths = "options")
    Optional<Poll> findWithOptionByShareCode(String shareCode);

    Optional<Poll> findByShareCode(String shareCode);

    @Query("SELECT p FROM Poll p WHERE COALESCE(p.closedAt, p.expiresAt) < :threshold")
    List<Poll> findAllClosedBefore(@Param("threshold") LocalDateTime threshold);
}
