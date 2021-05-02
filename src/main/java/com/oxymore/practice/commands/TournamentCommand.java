package com.oxymore.practice.commands;

import com.google.common.base.Preconditions;
import com.oxymore.practice.Practice;
import lombok.AllArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class TournamentCommand implements CommandExecutor {
    private final Practice plugin;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        Preconditions.checkArgument(commandSender instanceof Player);
        final Player player = (Player) commandSender;

        // TODO: remake tournaments
        // improvement goals:
        // - teleport to an in progress match, not spawn
        // - make a queue that actually works

        if (player.isOp()) {
            player.sendMessage(ChatColor.RED + "Tournaments have been removed for now, they were too unstable after testing. I will remake them at the same time as the replay.");
            player.sendMessage(ChatColor.RED + "Only OPs can see this message, don't worry.");
        } else {
            player.sendMessage(ChatColor.RED + "Tournaments are disabled for now.");
        }

        // REMINDER:
        // /home/jq/practice/no-vcs-bck/tour

        return true;
    }
}
