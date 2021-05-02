package com.oxymore.practice.commands;

import com.google.common.base.Preconditions;
import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.util.NMSUtil;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class PingCommand implements CommandExecutor {
    private final Practice plugin;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        Preconditions.checkArgument(commandSender instanceof Player, "you must be a player");
        final Player player = (Player) commandSender;
        final LocaleController locale = plugin.getLocale();

        final Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : player;
        if (target == null) {
            locale.get("command.ping.offline-target")
                    .send(commandSender);
            return true;
        }

        locale.get("command.elo.show")
                .var("target", target.getName())
                .var("elo", String.valueOf(NMSUtil.getPing(target)))
                .send(commandSender);
        return true;
    }
}
