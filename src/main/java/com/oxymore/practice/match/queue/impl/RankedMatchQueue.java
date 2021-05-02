package com.oxymore.practice.match.queue.impl;

import com.oxymore.practice.match.queue.AbstractMatchQueue;
import com.oxymore.practice.match.queue.QueueMatch;

public class RankedMatchQueue extends AbstractMatchQueue<RankedMatchQueueEntry> {
    public RankedMatchQueue(int requiredPlayers) {
        super(requiredPlayers);
    }

    @Override
    protected QueueMatch findMatch(RankedMatchQueueEntry entry) {
        return findMatchByPredicate(entry, getDefaultMatchPredicate(entry).and(itEntry -> {
            final int eloDiff = Math.abs(entry.getElo() - itEntry.getElo());
            return eloDiff <= entry.getRange() && eloDiff <= itEntry.getRange();
        }));
    }
}
