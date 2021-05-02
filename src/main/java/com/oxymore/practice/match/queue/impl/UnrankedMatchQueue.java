package com.oxymore.practice.match.queue.impl;

import com.oxymore.practice.match.queue.AbstractMatchQueue;
import com.oxymore.practice.match.queue.QueueMatch;

public class UnrankedMatchQueue extends AbstractMatchQueue<MatchQueueEntry> {
    public UnrankedMatchQueue(int requiredPlayers) {
        super(requiredPlayers);
    }

    @Override
    protected QueueMatch findMatch(MatchQueueEntry entry) {
        return findMatchByPredicate(entry, getDefaultMatchPredicate(entry));
    }
}
