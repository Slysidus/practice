package com.oxymore.practice.view.match;

import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.configuration.ui.ItemPlaceholder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
@Getter
public abstract class AbstractModeSelectorView implements InventoryHolder {
    protected final Practice plugin;
    protected final List<MatchMode> selectModes;
    protected final String prefix;

    private final Inventory inventory;

    public AbstractModeSelectorView(Practice plugin, List<MatchMode> selectModes, String prefix) {
        this.plugin = plugin;
        this.selectModes = selectModes;
        this.prefix = prefix;

        final int rows = (int) Math.ceil(Math.max(selectModes.size(), 1) / 9.);
        this.inventory = Bukkit.createInventory(this, rows * 9, plugin.getLocale().get(prefix + ".title").toString());
    }

    public void load() {
        for (int i = 0; i < selectModes.size(); i++) {
            inventory.setItem(i, getIcon(selectModes.get(i)));
        }
    }

    public ItemStack getIcon(MatchMode mode) {
        final MaterialData iconData = mode.neutralIcon;
        final ItemPlaceholder iconPlaceholder = new ItemPlaceholder(iconData.getItemType(), iconData.getData(), 1,
                "${" + prefix + ".mode.title}", "${" + prefix + ".mode.desc}");

        final Map<String, String> variables = new HashMap<>();
        variables.put("id", mode.id);
        variables.put("mode", mode.name);

        return iconPlaceholder.buildFromVariables(plugin.getLocale(), variables);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}
