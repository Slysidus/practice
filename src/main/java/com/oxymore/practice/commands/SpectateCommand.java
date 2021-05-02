package com.oxymore.practice.commands;

import com.google.common.base.Preconditions;
import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.match.Match;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class SpectateCommand implements CommandExecutor {
    private final Practice plugin;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        Preconditions.checkArgument(commandSender instanceof Player, "you must be a player");

        final Player player = (Player) commandSender;
        final LocaleController locale = plugin.getLocale();
        if (args.length == 0) {
            locale.get("command.spectate.missing-target")
                    .send(commandSender);
            return true;
        }

        if (plugin.getMatchingController().isInMatch(player)) {
            locale.get("command.spectate.in-match")
                    .send(commandSender);
            return true;
        }

        final Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            locale.get("command.spectate.offline-target")
                    .send(commandSender);
            return true;
        }

        if (!plugin.getMatchingController().isInMatch(target)) {
            locale.get("command.spectate.target-not-in-match")
                    .send(commandSender);
            return true;
        }

        final Match match = plugin.getMatchingController().getCurrentMatch(target);
        plugin.getMatchingController().spectate(player, match);
        plugin.getMatchingController().autoGo(player);
        locale.get("command.spectate.done")
                .var("target", target.getName())
                .send(player);
        return true;
    }
}
