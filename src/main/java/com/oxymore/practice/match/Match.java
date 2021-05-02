package com.oxymore.practice.match;

import com.oxymore.practice.LocaleController;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.match.arena.MatchArena;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class Match {
    private final MatchMode mode;
    private final MatchType matchType;
    private final MatchArena matchArena;

    private final Edited edited;
    private MatchState state;
    private int playtime;

    private final MatchTeam team1;
    private final MatchTeam team2;
    private final List<OfflinePlayer> dead;
    private final List<Player> spectators;

    private final Location centerLocation;
    private final Map<MatchTeam, Location> spawnLocations;

    private final Map<OfflinePlayer, PlayerMatchStats> playerStatistics;

    private final MatchBroadcaster broadcaster;

    public Match(MatchMode mode, MatchType matchType, MatchArena matchArena, Location centerLocation,
                 Map<MatchTeam, Location> spawnLocations, MatchTeam team1, MatchTeam team2, MatchBroadcaster broadcaster) {
        this.mode = mode;
        this.matchType = matchType;
        this.matchArena = matchArena;
        this.centerLocation = centerLocation;
        this.spawnLocations = spawnLocations;
        this.edited = new Edited(new HashMap<>(), new ArrayList<>());
        this.state = MatchState.STARTING;
        this.playtime = -1;
        this.team1 = team1;
        this.team2 = team2;
        this.dead = new ArrayList<>();
        this.spectators = new ArrayList<>();
        this.playerStatistics = new HashMap<>();
        getFightingPlayers().forEach(player -> playerStatistics.put(player, new PlayerMatchStats()));
        this.broadcaster = broadcaster;
    }

    // this may be a bit overkill
    // locale controller can be queried almost anywhere so I guess TODO: remove broadcaster
    public void broadcast(MatchTeam team, Function<LocaleController, String> getMessage) {
        broadcaster.broadcast(getReceivers(team), getMessage);
    }

    public Collection<Player> getReceivers(MatchTeam team) {
        final Collection<Player> receivers;
        if (team != null) {
            receivers = new ArrayList<>(team.getFightingPlayers());
        } else {
            receivers = new ArrayList<>();
            receivers.addAll(team1.getFightingPlayers());
            if (team2 != null) {
                receivers.addAll(team2.getFightingPlayers());
            }
            receivers.addAll(spectators);
        }
        return receivers;
    }

    public MatchTeam getTeam(OfflinePlayer player) {
        if (team1.isInTeam(player.getUniqueId())) {
            if (team2 == null) {
                if (dead.contains(player)) {
                    return new MatchTeam(new ArrayList<>(), Collections.singletonList(player));
                }
                return new MatchTeam(Collections.singletonList(player.getPlayer()), new ArrayList<>());
            }
            return team1;
        }
        if (team2 != null && team2.isInTeam(player.getUniqueId())) {
            return team2;
        }
        return null;
    }

    public MatchTeam getOpposingTeam(OfflinePlayer player) {
        if (team2 == null && team1.isInTeam(player.getUniqueId())) {
            return new MatchTeam(
                    team1.getFightingPlayers().stream()
                            .filter(itPlayer -> !itPlayer.equals(player))
                            .collect(Collectors.toList()),
                    dead.stream()
                            .filter(itPlayer -> !itPlayer.equals(player))
                            .collect(Collectors.toList())
            );
        }

        final MatchTeam team = getTeam(player);
        if (team == null) {
            return null;
        }

        if (team.equals(team1)) {
            return team2;
        } else if (team.equals(team2)) {
            return team1;
        }
        return null;
    }

    public MatchTeam getOpposingTeam(MatchTeam team) {
        if (team == null) {
            return null;
        }
        if (team.equals(team1)) {
            if (team2 == null && team1.getFightingPlayers().size() == 1) {
                return getOpposingTeam(team1.getFightingPlayers().get(0));
            }
            return team2;
        } else if (team.equals(team2)) {
            return team1;
        } else {
            return getOpposingTeam(team.getFightingPlayers().get(0));
        }
    }

    public Collection<MatchTeam> getTeams() {
        return Arrays.asList(team1, team2);
    }

    public List<Player> getFightingPlayers() {
        final List<Player> fightingPlayers = new ArrayList<>();
        fightingPlayers.addAll(team1.getFightingPlayers());
        if (team2 != null) {
            fightingPlayers.addAll(team2.getFightingPlayers());
        }
        return fightingPlayers;
    }

    public boolean isAlive(Player player) {
        final MatchTeam playerTeam = getTeam(player);
        return playerTeam != null && playerTeam.getFightingPlayers().contains(player);
    }

    public enum DeathCause {
        KILLED,
        FORFEIT,
    }

    public interface MatchBroadcaster {
        void broadcast(Collection<Player> receivers, Function<LocaleController, String> getMessage);
    }

    public void teleportToSpawn(Player player) {
        final MatchTeam team = team1.isInTeam(player.getUniqueId()) ? team1 : team2;
        final Location teleportLocation = spawnLocations.get(team).clone();
        if (centerLocation != null) {
            final Vector direction = centerLocation.toVector()
                    .subtract(teleportLocation.clone().add(0, 1, 0).toVector()).normalize();
            teleportLocation.setDirection(direction);
        }
        player.teleport(teleportLocation.clone().add(0, 0.05, 0));
    }

    @Data
    public static class Edited {
        private final Map<Location, MaterialData> blocks;
        private final List<Item> droppedItem;
    }
}
