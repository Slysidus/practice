package com.oxymore.practice.match;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class MatchTeam {
    private final List<Player> fightingPlayers;
    private final List<OfflinePlayer> deadPlayers;

    public MatchTeam(Player... players) {
        this.fightingPlayers = new ArrayList<>(Arrays.asList(players));
        this.deadPlayers = new ArrayList<>();
    }

    public List<OfflinePlayer> getPlayers() {
        final List<OfflinePlayer> players = new ArrayList<>();
        players.addAll(fightingPlayers);
        players.addAll(deadPlayers);
        return players;
    }

    public boolean isInTeam(UUID playerId) {
        Preconditions.checkNotNull(playerId);
        return isPresentInList(fightingPlayers, playerId) || isPresentInList(deadPlayers, playerId);
    }

    private <T extends OfflinePlayer> boolean isPresentInList(List<T> list, UUID playerId) {
        return list.stream()
                .anyMatch(player -> player.getUniqueId().equals(playerId));
    }
}
