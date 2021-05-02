package com.oxymore.practice.match.queue;

import com.google.common.base.Preconditions;
import com.oxymore.practice.match.queue.impl.MatchQueueEntry;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

public abstract class AbstractMatchQueue<T extends MatchQueueEntry> implements MatchQueue<T> {
    @Getter
    protected final int requiredPlayers;
    protected final Queue<T> queue;

    public AbstractMatchQueue(int requiredPlayers) {
        this.requiredPlayers = requiredPlayers;
        this.queue = new LinkedList<>();
    }

    @Override
    public void push(T entry) {
        Preconditions.checkArgument(!queue.contains(entry));
        final int playerCount = entry.getPlayers().size();
        Preconditions.checkArgument(playerCount > 0 && playerCount <= requiredPlayers);
        queue.add(entry);
    }

    @Override
    public T deQueuePlayer(Player player) {
        final T entry = getPlayerEntry(player);
        if (entry != null) {
            queue.remove(entry);
        }
        return entry;
    }

    @Override
    public T getPlayerEntry(Player player) {
        return queue.stream()
                .filter(entry -> entry.getPlayers().contains(player))
                .findAny().orElse(null);
    }

    @Override
    public List<QueueMatch> searchMatches() {
        final List<QueueMatch> matches = new ArrayList<>();

        final List<T> checked = new ArrayList<>();
        while (checked.size() != queue.size()) {
            final Iterator<T> it = queue.iterator();
            // use iterator to prevent java complaining about removing while iterating
            //noinspection WhileLoopReplaceableByForEach
            while (it.hasNext()) {
                final T entry = it.next();
                if (checked.contains(entry)) {
                    continue;
                }
                final QueueMatch match = findMatch(entry);
                if (match != null) {
                    queue.removeIf(match.getEntries()::contains);
                    matches.add(match);
                    break;
                } else {
                    checked.add(entry);
                }
            }
        }

        return matches;
    }

    @Override
    public int count(Predicate<T> predicate) {
        return queue.stream()
                .filter(predicate)
                .mapToInt(itEntry -> itEntry.getPlayers().size())
                .sum();
    }

    @Override
    public Queue<T> getEntries() {
        return new LinkedBlockingQueue<>(queue);
    }

    protected abstract QueueMatch findMatch(T entry);

    protected final QueueMatch findMatchByPredicate(T entry, Predicate<T> predicate) {
        final List<T> entries = new ArrayList<>();
        entries.add(entry);
        int playerCount = entry.getPlayers().size();

        final Iterator<T> iterator = queue.iterator();
        while (iterator.hasNext() && playerCount < requiredPlayers) {
            final T itEntry = iterator.next();
            if (entries.contains(itEntry)) {
                continue;
            }

            final int itPlayerCount = itEntry.getPlayers().size();
            if (itPlayerCount > requiredPlayers - playerCount) {
                continue;
            }

            if (predicate.test(itEntry)) {
                playerCount += itPlayerCount;
                entries.add(itEntry);
            }
        }

        if (playerCount < requiredPlayers) {
            return null;
        }
        return new QueueMatch(entries, entry.getMatchMode(), entry.getArena());
    }

    protected final Predicate<T> getDefaultMatchPredicate(MatchQueueEntry entry) {
        return itEntry -> {
            final boolean matchArena = (entry.getArena() == null || itEntry.getArena() == null) ||
                    entry.getArena().equals(itEntry.getArena());
            return entry.getMatchMode().equals(itEntry.getMatchMode()) && matchArena;
        };
    }
}
