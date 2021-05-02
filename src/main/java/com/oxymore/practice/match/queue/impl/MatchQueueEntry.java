package com.oxymore.practice.match.queue.impl;

import com.oxymore.practice.configuration.match.MatchMode;
import lombok.Data;
import org.bukkit.entity.Player;

import java.util.Collection;

@Data
public class MatchQueueEntry {
    private final Collection<Player> players;
    private final MatchMode matchMode;
    private final String arena;
}
