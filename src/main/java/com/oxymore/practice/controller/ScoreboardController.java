package com.oxymore.practice.controller;

import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.documents.PlayerDocument;
import fr.vinetos.util.scoreboard.ScoreboardSign;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Function;

@Getter
public final class ScoreboardController {
    private final Practice plugin;
    private final LocaleController locale;

    private final List<Player> inLobby;
    private final Map<Player, ScoreboardSign> playerScoreboards;

    private final ChatColor[] cachedChatColors;

    public ScoreboardController(Practice plugin) throws ControllerInitException {
        this.plugin = plugin;
        this.locale = plugin.getLocale();
        this.inLobby = new ArrayList<>();
        this.playerScoreboards = new HashMap<>();
        this.cachedChatColors = ChatColor.values();
    }

    public void updateScoreboard(Player player, Function<LocaleController, String> getMessage) {
        if (!playerScoreboards.containsKey(player)) {
            final ScoreboardSign scoreboard = new ScoreboardSign(player.getUniqueId(), locale.get("scoreboard.title").toString(), ScoreboardSign.ScoreboardSlot.SIDEBAR);
            scoreboard.create();
            playerScoreboards.put(player, scoreboard);
        }
        final ScoreboardSign scoreboard = playerScoreboards.get(player);
        updateScoreboard(scoreboard, getMessage.apply(locale));
    }

    public void destroyScoreboard(Player player) {
        inLobby.remove(player);
        final ScoreboardSign scoreboard = playerScoreboards.remove(player);
        if (scoreboard != null) {
            scoreboard.destroy();
        }
    }

    private void updateScoreboard(ScoreboardSign scoreboard, String content) {
        // use lines as a stack FIFO
        final List<String> lines = new ArrayList<>(Arrays.asList(content.split("\n")));
        ChatColor uniquify = ChatColor.BLACK;
        for (int i = 0; i < 15; i++) {
            String currentLine = scoreboard.getLine(i);
            if (currentLine != null && currentLine.isEmpty()) {
                currentLine = null;
            }
            String newLine = lines.isEmpty() ? null : lines.remove(0);
            // line is present more than once, need to be made unique
            // if the line is empty make it unique anyway to prevent conflicts with empty current line being set to null above
            if (newLine != null && (lines.contains(newLine) || newLine.isEmpty())) {
                newLine = uniquify.toString() + newLine;
                uniquify = cachedChatColors[uniquify.ordinal() + 1];
            }
            if (currentLine == null && newLine == null) {
                continue;
            }

            if (currentLine == null || !currentLine.equals(newLine)) {
                if (newLine == null) {
                    scoreboard.removeLine(i);
                } else {
                    scoreboard.setLine(i, newLine);
                }
            }
        }
    }

    public void updateLobbyScoreboard() {
        inLobby.forEach(this::updateLobbyScoreboard);
    }

    private void updateLobbyScoreboard(Player player) {
        final DatabaseController databaseController = plugin.getDatabaseController();
        final int elo = databaseController.getEloCache().getOrDefault(player.getUniqueId(), 1000);
        final PlayerDocument playerDocument = databaseController.getPlayerCache().get(player.getUniqueId());
        final String kdr = playerDocument != null && (playerDocument.getDeaths() > 0 || playerDocument.getKills() > 0)
                ? playerDocument.getDeaths() == 0 && playerDocument.getKills() > 0
                ? "inf." : String.valueOf(((int) (playerDocument.getKills() * 1. / playerDocument.getDeaths() * 10)) / 10.)
                : "N/A";
        updateScoreboard(player, locale -> locale.get("scoreboard.spawn.content")
                .var("username", player.getName())
                .var("online", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .var("fighting", String.valueOf(plugin.getMatchingController().getFightingPlayerCount()))
                .var("elo", String.valueOf(elo))
                .var("kdr", kdr)
                .toString());
    }

    public void toLobby(Player player) {
        if (inLobby.contains(player)) {
            return;
        }
        inLobby.add(player);
        updateLobbyScoreboard(player);
    }
}
