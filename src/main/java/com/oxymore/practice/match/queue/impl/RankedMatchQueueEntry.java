package com.oxymore.practice.match.queue.impl;

import com.oxymore.practice.configuration.match.MatchMode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.entity.Player;

import java.util.Collection;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class RankedMatchQueueEntry extends MatchQueueEntry {
    private final int elo;
    private int range;
    private int rangeTimer;

    public RankedMatchQueueEntry(Collection<Player> players, MatchMode matchMode, String arena, int elo) {
        super(players, matchMode, arena);
        this.elo = elo;
        this.range = 0;
        this.rangeTimer = 0;
    }
}
