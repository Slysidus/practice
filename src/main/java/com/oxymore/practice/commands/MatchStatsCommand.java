package com.oxymore.practice.commands;

import com.google.common.base.Preconditions;
import com.oxymore.practice.Practice;
import com.oxymore.practice.controller.ViewController;
import com.oxymore.practice.view.ViewContext;
import com.oxymore.practice.view.gui.PlayerMatchStatsGUI;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@AllArgsConstructor
public class MatchStatsCommand implements CommandExecutor {
    private final Practice plugin;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        Preconditions.checkArgument(commandSender instanceof Player, "you must be a player");
        final Player player = (Player) commandSender;

        final String uuidStr = args.length > 0 ? args[0] : "";
        final UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException ignored) {
            plugin.getLocale().get("match.stats.invalid-uuid")
                    .send(commandSender);
            return true;
        }

        final PlayerMatchStatsGUI.PlayerMatchStatsSnapshot snapshot = plugin.getMatchingController().getPlayerSnapshots().get(uuid);
        if (snapshot != null) {
            plugin.getViewPanel(player)
                    .setGUIView(ViewController.GUIViews.STATS, ViewContext.builder()
                            .locale(plugin.getLocale())
                            .data(snapshot)
                            .build());
        } else {
            plugin.getLocale().get("match.stats.expired")
                    .send(commandSender);
        }
        return true;
    }
}
