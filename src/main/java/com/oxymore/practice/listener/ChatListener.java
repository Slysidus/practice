package com.oxymore.practice.listener;

import com.oxymore.practice.Practice;
import com.oxymore.practice.documents.KitDocument;
import com.oxymore.practice.match.party.Party;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public final class ChatListener implements Listener {
    private final static Pattern KIT_NAME_PATTERN;

    static {
        KIT_NAME_PATTERN = Pattern.compile("^\\w+{1,20}$");
    }

    private final Practice plugin;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChatLowest(AsyncPlayerChatEvent event) {
        if (plugin.getKitRenaming().containsKey(event.getPlayer())) {
            event.setCancelled(true);

            final Player player = event.getPlayer();
            final KitDocument kitDocument = plugin.getKitRenaming().remove(player);
            if (ChatListener.KIT_NAME_PATTERN.matcher(event.getMessage()).find()) {
                final String initialName = kitDocument.getDisplayName();
                plugin.getDatabaseController().async(db -> {
                    kitDocument.setName(event.getMessage());
                    db.saveKit(kitDocument);
                    plugin.getLocale().get("kit.rename.done")
                            .var("previous", initialName)
                            .var("new", kitDocument.getDisplayName())
                            .var("slot", String.valueOf(kitDocument.getSlot()))
                            .send(player);
                });
            } else {
                plugin.getLocale().get("kit.rename.invalid")
                        .send(player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.getMessage().startsWith("?")) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                final Party party = plugin.getPartyController().getParty(event.getPlayer());
                if (party != null) {
                    party.broadcast(plugin.getLocale().get("party.chat.entry")
                            .var("player", event.getPlayer().getName())
                            .var("message", event.getMessage().substring(1)));
                } else {
                    plugin.getLocale().get("party.chat.none")
                            .send(event.getPlayer());
                }
            });
        }
    }
}
