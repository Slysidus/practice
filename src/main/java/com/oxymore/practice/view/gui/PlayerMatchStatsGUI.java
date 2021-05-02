package com.oxymore.practice.view.gui;

import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.controller.ViewController;
import com.oxymore.practice.match.PlayerMatchStats;
import com.oxymore.practice.view.ViewContext;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMatchStatsGUI extends GUIView {
    public PlayerMatchStatsGUI(ViewConfiguration viewConfiguration) {
        super(viewConfiguration);
    }

    @Override
    public void show(Player player, ViewContext viewContext) {
        super.show(player, viewContext);

        final PlayerMatchStatsSnapshot snapshot = (PlayerMatchStatsSnapshot) viewContext.getData();
        final PlayerMatchStats stats = snapshot.matchStats;

        final Map<String, String> variables = new HashMap<>();
        variables.put("hits", String.valueOf(stats.hits));
        variables.put("combo", String.valueOf(stats.combo));
        variables.put("longest-combo", String.valueOf(stats.longestCombo));
        variables.put("potions-used", String.valueOf(stats.potions));
        variables.put("potions-left", String.valueOf(snapshot.potionsLeft));
        variables.put("hearts", String.valueOf(((int) (snapshot.health / 2 * 10)) / 10.));
        variables.put("player", snapshot.player.getName());

        final Inventory inventory = setupInventory(ViewContext.builder()
                .locale(viewContext.getLocale())
                .variables(new LocaleController.LocaleVariables(variables, null))
                .build());

        for (int i = 0; i < snapshot.armorContents.length; i++) {
            final ItemStack content = snapshot.armorContents[i];
            inventory.setItem(8 - i, content);
        }
        for (int i = 0; i < snapshot.contents.length; i++) {
            final ItemStack content = snapshot.contents[i];
            final int slot = 9 + (i / 9 == 0 ? 3 * 9 + i : i - 9);
            inventory.setItem(slot, content);
        }
        player.openInventory(inventory);
    }

    @Override
    public void performAction(Practice plugin, Player player, String action) {
        switch (action) {
            case "close": {
                player.closeInventory();
                break;
            }
            case "next-player":
            case "previous-player": {
                final boolean next = action.startsWith("next");

                final ViewContext ctx = playerContexts.get(player.getUniqueId());
                final PlayerMatchStatsSnapshot snapshot = (PlayerMatchStatsSnapshot) ctx.getData();
                final UUID targetSnapshotId = next ? snapshot.next : snapshot.previous;
                final PlayerMatchStatsSnapshot targetSnapshot = targetSnapshotId != null ? plugin.getMatchingController()
                        .getPlayerSnapshots().get(targetSnapshotId) : null;
                if (targetSnapshot != null) {
                    player.closeInventory();
                    plugin.getViewPanel(player).setGUIView(ViewController.GUIViews.STATS, ViewContext.builder()
                            .locale(ctx.getLocale())
                            .data(targetSnapshot)
                            .build());
                }
                break;
            }
        }
    }

    @Data
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    public static class PlayerMatchStatsSnapshot {
        public final UUID id;
        public final OfflinePlayer player;
        public final ItemStack[] contents;
        public final ItemStack[] armorContents;
        public final int potionsLeft;
        public final double health;
        public final PlayerMatchStats matchStats;

        public UUID previous;
        public UUID next;

        public PlayerMatchStatsSnapshot(UUID id, OfflinePlayer player, ItemStack[] contents, ItemStack[] armorContents,
                                        double health, PlayerMatchStats matchStats) {
            this.id = id;
            this.player = player;
            this.contents = contents;
            this.armorContents = armorContents;
            this.potionsLeft = Arrays.stream(contents)
                    .filter(itm -> itm != null && itm.getType() == Material.POTION)
                    .filter(itm -> itm.getDurability() > 16348)
                    .map(ItemStack::getAmount).mapToInt(Integer::intValue)
                    .sum();
            this.health = health;
            this.matchStats = matchStats;
        }
    }
}
