package com.oxymore.practice.commands;

import com.google.common.base.Preconditions;
import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.view.misc.LeaderboardView;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class LeaderboardCommand implements CommandExecutor {
    private final Practice plugin;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        Preconditions.checkArgument(commandSender instanceof Player, "you must be a player");
        final Player player = (Player) commandSender;

        final List<MatchMode> modes = new ArrayList<>(plugin.getConfiguration().matchModes);
        plugin.getDatabaseController().async(db -> {
            final LeaderboardView leaderboardView = new LeaderboardView(plugin, modes);
            leaderboardView.load(db);
            db.syncIfOnline(player, () -> {
                player.closeInventory();
                leaderboardView.open(player);
            });
        });
        return true;
    }
}
