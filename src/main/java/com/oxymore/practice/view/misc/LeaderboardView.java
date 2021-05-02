package com.oxymore.practice.view.misc;

import com.mongodb.client.model.Filters;
import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.configuration.ui.ItemPlaceholder;
import com.oxymore.practice.controller.DatabaseController;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.List;

@SuppressWarnings("deprecation")
@Getter
public final class LeaderboardView implements InventoryHolder {
    private final Practice plugin;
    private final List<MatchMode> modes;
    private final Inventory inventory;

    public LeaderboardView(Practice plugin, List<MatchMode> modes) {
        this.plugin = plugin;
        this.modes = modes;

        final int rows = (int) Math.ceil(Math.max(modes.size(), 1) / 9.) + 1;
        this.inventory = Bukkit.createInventory(this, rows * 9, plugin.getLocale().get("leaderboard.title").toString());
    }

    public void load(DatabaseController.Database db) {
        inventory.setItem(4, bakeIcon(null, db));
        for (int i = 0; i < modes.size(); i++) {
            inventory.setItem(i + 9, bakeIcon(modes.get(i), db));
        }
    }

    private ItemStack bakeIcon(MatchMode mode, DatabaseController.Database db) {
        final String modeId = mode != null ? mode.id : "Global";
        final String modeName = mode != null ? mode.name : "Global";

        final MaterialData iconData = mode != null ? mode.neutralIcon : new MaterialData(Material.NETHER_STAR);
        final ItemPlaceholder iconPlaceholder = new ItemPlaceholder(iconData.getItemType(), iconData.getData(), 1,
                "${leaderboard.entry.title}", "${leaderboard.entry.desc}");

        return iconPlaceholder.build(
                title -> plugin.getLocale().makeContext(title)
                        .var("id", modeId)
                        .var("mode", modeName)
                        .toString(),
                desc -> plugin.getLocale().makeContext(desc)
                        .var("id", modeId)
                        .var("mode", modeName)
                        .expansion("entries", db.fetchTop(mode != null ? Filters.eq("mode", modeId) : null))
                        .toString()
        );
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}
