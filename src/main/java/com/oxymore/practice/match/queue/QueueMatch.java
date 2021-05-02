package com.oxymore.practice.match.queue;

import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.match.queue.impl.MatchQueueEntry;
import lombok.Data;

import java.util.List;

@Data
public class QueueMatch {
    private final List<? extends MatchQueueEntry> entries;
    private final MatchMode matchMode;
    private final String arena;
}
