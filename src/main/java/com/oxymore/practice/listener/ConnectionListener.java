package com.oxymore.practice.listener;

import com.mongodb.client.model.Filters;
import com.oxymore.practice.Practice;
import com.oxymore.practice.documents.PlayerDocument;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AllArgsConstructor
public final class ConnectionListener implements Listener {
    private final Practice plugin;

    @EventHandler(ignoreCancelled = true)
    public void onConnect(PlayerJoinEvent event) {
        if (plugin.getConfiguration().hideConnectMessages) {
            event.setJoinMessage(null);
        }

        final Player player = event.getPlayer();
        plugin.getViewController().joinPlayer(player);
        plugin.getMatchingController().autoGo(player);
        plugin.getVisibilityController().onConnect(player);

        plugin.getDatabaseController().async(db -> {
            PlayerDocument playerDoc = db.players.find(Filters.eq("playerId", player.getUniqueId())).first();
            if (playerDoc == null) {
                playerDoc = new PlayerDocument(player.getUniqueId(), 0, 0);
                db.players.insertOne(playerDoc);
            }
            plugin.getDatabaseController().queryCacheUpdate(player, false);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDisconnect(PlayerQuitEvent event) {
        if (plugin.getConfiguration().hideConnectMessages) {
            event.setQuitMessage(null);
        }

        final Player player = event.getPlayer();
        plugin.getScoreboardController().destroyScoreboard(player);
        plugin.getViewController().quitPlayer(player);
        plugin.getMatchingController().onDisconnect(player);
        plugin.getDatabaseController().removeFromCache(player);
        plugin.getVisibilityController().onDisconnect(player);
    }
}
