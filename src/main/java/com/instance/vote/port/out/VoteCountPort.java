package com.instance.vote.port.out;

public interface VoteCountPort {
    void increment(Long pollId, Long optionId);
}
