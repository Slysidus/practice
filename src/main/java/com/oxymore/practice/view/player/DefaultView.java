package com.oxymore.practice.view.player;

import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.controller.ViewController;
import com.oxymore.practice.view.ViewContext;
import com.oxymore.practice.view.misc.KitModeSelectorView;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class DefaultView extends PlayerView {
    public DefaultView(ViewConfiguration viewConfiguration) {
        super(viewConfiguration);
    }

    @Override
    public void performAction(Practice plugin, Player player, String action) {
        switch (action) {
            case "party-create": {
                player.performCommand("party create");
                break;
            }
            case "join-unranked": {
                join(plugin, player, false);
                break;
            }
            case "join-ranked": {
                join(plugin, player, true);
                break;
            }
            case "leave-queue": {
                if (plugin.getMatchingController().isInQueue(player)) {
                    plugin.getMatchingController().removeFromQueue(player);
                    plugin.getLocale().get("join.queue-left")
                            .send(player);
                } else {
                    plugin.getLocale().get("join.not-in-queue")
                            .send(player);
                }
                break;
            }
            case "open-leaderboard": {
                player.performCommand("leaderboard");
                break;
            }
            case "edit-kits": {
                final KitModeSelectorView view = new KitModeSelectorView(plugin, plugin.getConfiguration().matchModes.stream()
                        .filter(mode -> mode.allowCustomKits).collect(Collectors.toList()));
                view.load();
                view.open(player);
            }
        }
    }

    private void join(Practice plugin, Player player, boolean ranked) {
        if (plugin.getMatchingController().isInQueue(player)) {
            plugin.getLocale().get("join.in-queue")
                    .send(player);
            return;
        }
        plugin.getViewPanel(player).setGUIView(ViewController.GUIViews.SELECT_AUX, ViewContext.builder()
                .locale(plugin.getLocale())
                .data(ranked)
                .build());
    }
}
