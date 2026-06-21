package com.instance.vote.repository;

import com.instance.vote.domain.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {
}
