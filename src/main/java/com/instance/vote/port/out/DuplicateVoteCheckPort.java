package com.instance.vote.port.out;

public interface DuplicateVoteCheckPort {
    boolean isFirstVote(Long pollId, String participantToken, long ttlSeconds);
    void deleteVoteKey(Long pollId, String participantToken);
}
