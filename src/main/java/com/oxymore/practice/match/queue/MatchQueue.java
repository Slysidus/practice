package com.oxymore.practice.match.queue;

import com.oxymore.practice.match.queue.impl.MatchQueueEntry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

public interface MatchQueue<T extends MatchQueueEntry> {
    int getRequiredPlayers();

    void push(T entry);

    T deQueuePlayer(Player player);

    T getPlayerEntry(Player player);

    List<QueueMatch> searchMatches();

    int count(Predicate<T> predicate);

    Queue<T> getEntries();

    default boolean isQueued(Player player) {
        return getPlayerEntry(player) != null;
    }
}
