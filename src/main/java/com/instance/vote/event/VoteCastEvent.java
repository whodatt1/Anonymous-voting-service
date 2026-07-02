package com.instance.vote.event;

public record VoteCastEvent(Long pollId, String shareCode) {
}
