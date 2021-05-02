package com.oxymore.practice.controller;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.oxymore.practice.Practice;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class VisibilityController {
    private final Practice practice;

    private final Multimap<Player, Player> restrainedVisibility;

    public VisibilityController(Practice practice) {
        this.practice = practice;
        this.restrainedVisibility = ArrayListMultimap.create();
    }

    public void onConnect(Player player) {
        for (Player itPlayer : restrainedVisibility.keys()) {
            itPlayer.hidePlayer(player);
        }
    }

    public void onDisconnect(Player player) {
        restrainedVisibility.removeAll(player);
        restrainedVisibility.values().removeIf(itPlayer -> itPlayer.equals(player));
    }

    public void restrain(Player player, Collection<Player> show) {
        restrainedVisibility.removeAll(player);
        restrainedVisibility.putAll(player, show);
        for (Player itPlayer : Bukkit.getOnlinePlayers()) {
            final boolean shouldSee = show.contains(itPlayer) || itPlayer.equals(player);
            if (shouldSee && !player.canSee(itPlayer)) {
                player.showPlayer(itPlayer);
            } else if (!shouldSee && player.canSee(player)) {
                player.hidePlayer(itPlayer);
            }
        }
    }

    public void hideIfShown(Player player, Player target) {
        if (restrainedVisibility.containsKey(player)) {
            restrainedVisibility.remove(player, target);
            player.hidePlayer(target);
        }
    }

    public void liftRestrain(Player player) {
        restrainedVisibility.removeAll(player);
        for (Player itPlayer : Bukkit.getOnlinePlayers()) {
            if (!player.canSee(itPlayer)) {
                player.showPlayer(itPlayer);
            }
        }
    }
}
