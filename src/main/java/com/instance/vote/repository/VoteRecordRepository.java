package com.instance.vote.repository;

import com.instance.vote.domain.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {

    record OptionCount(Long optionId, Long count) {}

    @Query("SELECT new com.instance.vote.repository.VoteRecordRepository$OptionCount" +
            "(v.option.id, COUNT(v)) " +
            "FROM VoteRecord v WHERE v.poll.id = :pollId GROUP BY v.option.id")
    List<OptionCount> countByPollIdGroupByOption(@Param("pollId") Long pollId);
}
