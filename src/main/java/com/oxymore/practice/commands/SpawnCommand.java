package com.oxymore.practice.commands;

import com.google.common.base.Preconditions;
import com.oxymore.practice.Practice;
import com.oxymore.practice.controller.MatchingController;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class SpawnCommand implements CommandExecutor {
    private final Practice plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Preconditions.checkArgument(sender instanceof Player);

        final Player player = (Player) sender;
        final MatchingController matchingController = plugin.getMatchingController();
        matchingController.resetState(player);
        matchingController.sendToSpawn(player);
        plugin.getLocale().get("command.spawn.done")
                .send(player);
        return true;
    }
}
