package com.oxymore.practice.tasks;

import com.google.common.base.Preconditions;
import com.oxymore.practice.LocaleController;
import com.oxymore.practice.configuration.PracticeConfiguration;
import com.oxymore.practice.controller.MatchingController;
import com.oxymore.practice.match.*;
import com.oxymore.practice.match.queue.MatchQueue;
import com.oxymore.practice.match.queue.QueueMatch;
import com.oxymore.practice.match.queue.impl.MatchQueueEntry;
import com.oxymore.practice.match.queue.impl.RankedMatchQueue;
import com.oxymore.practice.match.queue.impl.RankedMatchQueueEntry;
import com.oxymore.practice.util.NMSUtil;
import com.oxymore.practice.view.match.MatchSelector;
import lombok.AllArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MatchLoop implements Runnable {
    private final MatchingController matchingController;

    @Override
    public void run() {
        final LocaleController localeController = matchingController.getPlugin().getLocale();
        final PracticeConfiguration configuration = matchingController.getPlugin().getConfiguration();

        for (Map.Entry<MatchType, MatchQueue<?>> entry : matchingController.getQueues().entrySet()) {
            final MatchType matchType = entry.getKey();
            final MatchQueue<?> queue = entry.getValue();
            for (QueueMatch match : queue.searchMatches()) {
                final int playersPerTeam = queue.getRequiredPlayers() / 2;
                final Collection<Player> allOrderedPlayers = match.getEntries().stream()
                        .map(MatchQueueEntry::getPlayers)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

                final List<Player> team1 = allOrderedPlayers.stream()
                        .limit(playersPerTeam)
                        .collect(Collectors.toList());
                final List<Player> team2 = allOrderedPlayers.stream()
                        .skip(playersPerTeam)
                        .collect(Collectors.toList());

                team1.forEach(player -> localeController.get("join.found")
                        .var("team", team1.stream().map(Player::getName).collect(Collectors.joining(", ")))
                        .var("opponents", team2.stream().map(Player::getName).collect(Collectors.joining(", ")))
                        .send(player));
                team2.forEach(player -> localeController.get("join.found")
                        .var("team", team2.stream().map(Player::getName).collect(Collectors.joining(", ")))
                        .var("opponents", team1.stream().map(Player::getName).collect(Collectors.joining(", ")))
                        .send(player));

                try {
                    matchingController.createMatch(match.getMatchMode(), matchType, match.getArena(), team1, team2);
                } catch (MatchingController.MatchCreationException e) {
                    localeController.get(e.getMessage())
                            .send(allOrderedPlayers);
                }
            }

            if (queue instanceof RankedMatchQueue) {
                for (RankedMatchQueueEntry queueEntry : ((RankedMatchQueue) queue).getEntries()) {
                    if (queueEntry.getRange() == 0) {
                        queueEntry.setRange(configuration.eloSearch.start);
                    }

                    if (queueEntry.getRange() >= configuration.eloSearch.stop) {
                        continue;
                    }

                    queueEntry.setRangeTimer(queueEntry.getRangeTimer() + 1);
                    if (queueEntry.getRangeTimer() >= configuration.eloSearch.increaseInterval) {
                        queueEntry.setRange(queueEntry.getRange() + configuration.eloSearch.increase);
                        queueEntry.setRangeTimer(0);
                        localeController.get("join.elo-increase")
                                .var("range", String.valueOf(queueEntry.getRange()))
                                .send(queueEntry.getPlayers());
                    }
                }
            }
        }

        for (Match match : matchingController.getDistinctMatches()) {
            match.setPlaytime(match.getPlaytime() + 1);
            match.getPlayerStatistics().entrySet().stream()
                    .filter(entry -> entry.getValue().enderpearlCooldown > 0)
                    .forEach(entry -> {
                        entry.getValue().enderpearlCooldown--;
                        if (entry.getKey().isOnline()) {
                            final Player player = entry.getKey().getPlayer();
                            if (match.isAlive(player)) {
                                player.setLevel(entry.getValue().enderpearlCooldown);
                            }
                        }
                    });

            final String rankedStr = localeController.get("scoreboard.game." + (match.getMatchType().isRanked() ? "ranked" : "unranked"))
                    .toString();
            for (Player player : match.getFightingPlayers()) {
                final MatchTeam opposingTeam = match.getOpposingTeam(player);
                Preconditions.checkNotNull(opposingTeam);
                matchingController.getPlugin().getScoreboardController()
                        .updateScoreboard(player, locale -> locale.get("scoreboard.game.content")
                                .var("username", player.getName())
                                .var("mode", match.getMode().name)
                                .var("mode_aux", match.getMatchType().getAux())
                                .var("ranked", rankedStr)
                                .var("ping", String.valueOf(NMSUtil.getPing(player)))
                                .expansion("opponents", opposingTeam.getFightingPlayers().stream()
                                        .map(opponent -> (LocaleController.ExpansionElement) ctx -> ctx
                                                .var("player", opponent.getName())
                                                .var("ping", String.valueOf(NMSUtil.getPing(opponent)))
                                        )
                                        .limit(6)
                                        .collect(Collectors.toList()))
                                .toString());
            }
            for (Player player : match.getSpectators()) {
                matchingController.getPlugin().getScoreboardController()
                        .updateScoreboard(player, locale -> locale.get("scoreboard.spectate.content")
                                .var("username", player.getName())
                                .var("mode", match.getMode().name)
                                .var("mode_aux", match.getMatchType().getAux())
                                .var("ranked", rankedStr)
                                .var("ping", String.valueOf(NMSUtil.getPing(player)))
                                .toString());
            }

            if (match.getState() == MatchState.STARTING) {
                if (match.getPlaytime() == 5) {
                    match.setPlaytime(0);
                    match.broadcast(null, locale -> locale.get("match.game.started").toString());
                    match.setState(MatchState.PLAYING);
                    match.getFightingPlayers().forEach(player -> matchingController.stuff(match, player));
                } else {
                    match.getFightingPlayers().forEach(player ->
                            player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1, 1));
                    match.broadcast(null, locale -> locale.get("match.game.start-timer")
                            .var("seconds", String.valueOf(5 - match.getPlaytime()))
                            .toString());
                }
            }

            if (match.getState() == MatchState.ENDED) {
                if (match.getPlaytime() == configuration.endTeleportTime) {
                    matchingController.getCurrentMatches().values().removeIf(it -> it.equals(match));
                    matchingController.getSpectating().values().removeIf(it -> it.equals(match));
                    match.getReceivers(null).forEach(matchingController.getPlugin().getVisibilityController()::liftRestrain);
                    match.getReceivers(null).forEach(matchingController::sendToSpawn);
                    matchingController.getPlugin().getArenaController().releaseMatchArena(match.getMatchArena());

                    //noinspection deprecation
                    match.getEdited().getBlocks().forEach((location, data) -> location.getBlock().setTypeIdAndData(data.getItemTypeId(), data.getData(), true));
                    match.getEdited().getDroppedItem().forEach(Entity::remove);
                }
            }
        }

        matchingController.getSelectors().values().forEach(MatchSelector::update);
        matchingController.getPlugin().getScoreboardController().updateLobbyScoreboard();
    }
}
