package com.instance.vote.port.out;

import java.util.List;
import java.util.Map;

public interface VoteCountReadPort {
    Map<Long, Long> getCounts(Long pollId, List<Long> optionIds);
}
