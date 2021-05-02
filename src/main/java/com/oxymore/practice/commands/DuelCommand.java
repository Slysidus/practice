package com.oxymore.practice.commands;

import com.google.common.base.Preconditions;
import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.controller.MatchingController;
import com.oxymore.practice.match.MatchType;
import com.oxymore.practice.view.match.ModeSelectorView;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DuelCommand implements CommandExecutor, ModeSelectorView.ModeSelectedConsumer {
    private final Practice plugin;

    private final ModeSelectorView modeSelector;
    private final Map<Player, DuelRequest> duelRequests;

    public DuelCommand(Practice plugin) {
        this.plugin = plugin;

        this.modeSelector = new ModeSelectorView(plugin, plugin.getConfiguration().matchModes.stream().filter(mode -> mode.supportsDuel)
                .collect(Collectors.toList()), MatchType.UNRANKED_1v1, this);
        modeSelector.load();
        this.duelRequests = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        Preconditions.checkArgument(commandSender instanceof Player, "you must be a player");

        final LocaleController locale = plugin.getLocale();
        if (args.length == 0) {
            locale.get("duel.missing-target")
                    .send(commandSender);
            return true;
        }

        final Player player = (Player) commandSender;
        if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {
            final boolean accept = args[0].equalsIgnoreCase("accept");

            final Player sender = Bukkit.getPlayer(args[1]);
            final DuelRequest duelRequest = sender != null ? duelRequests.get(sender) : null;
            if (duelRequest == null || !duelRequest.target.equals(player)) {
                locale.get("duel.no-request")
                        .send(Objects.requireNonNull(sender));
                return true;
            }

            duelRequests.remove(sender);
            if (plugin.getMatchingController().isInMatch(sender)) {
                locale.get("duel.sender-in-match")
                        .send(commandSender);
                return true;
            }

            final String state = accept ? "accepted" : "denied";
            locale.get("duel.request." + state + ".target")
                    .var("sender", duelRequest.sender.getName())
                    .send(duelRequest.target);
            locale.get("duel.request." + state + ".sender")
                    .var("target", duelRequest.target.getName())
                    .send(duelRequest.sender);

            if (accept) {
                try {
                    plugin.getMatchingController().createMatch(duelRequest.matchMode, MatchType.UNRANKED_1v1, duelRequest.arena,
                            Collections.singletonList(duelRequest.sender), Collections.singletonList(duelRequest.target));
                } catch (MatchingController.MatchCreationException e) {
                    locale.get(e.getMessage())
                            .send(duelRequest.target);
                    locale.get(e.getMessage())
                            .send(duelRequest.sender);
                }
            }
            return true;
        }

        final Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            locale.get("duel.offline-target")
                    .send(commandSender);
            return true;
        }

        if (target.equals(player)) {
            locale.get("duel.target-yourself")
                    .send(commandSender);
            return true;
        }

        if (plugin.getMatchingController().isInMatch(target)) {
            locale.get("duel.target-in-match")
                    .send(commandSender);
            return true;
        }

        duelRequests.put(player, new DuelRequest(null, null, player, target));
        modeSelector.open(player);
        return true;
    }

    @Override
    public void onModeSelect(Player player, MatchType matchType, MatchMode matchMode, String arena) {
        final DuelRequest duelRequest = duelRequests.get(player);
        duelRequest.matchMode = matchMode;
        duelRequest.arena = arena;

        final LocaleController locale = plugin.getLocale();
        locale.get("duel.request.sender")
                .var("mode", matchMode.name)
                .var("target", duelRequest.target.getName())
                .send(duelRequest.sender);
        locale.get("duel.request.target")
                .var("mode", matchMode.name)
                .var("sender", duelRequest.sender.getName())
                .send(duelRequest.target);
    }

    @AllArgsConstructor
    static class DuelRequest {
        private MatchMode matchMode;
        private String arena;

        private final Player sender;
        private final Player target;
    }
}
