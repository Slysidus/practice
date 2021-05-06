package com.oxymore.practice.view.match;

import com.oxymore.practice.LocaleController;
import com.oxymore.practice.configuration.match.ArenaConfiguration;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.configuration.ui.ItemPlaceholder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class ArenaSelectorView implements InventoryHolder {
    private final ModeSelectorView modeSelectorView;
    private final List<ArenaConfiguration> selectArenas;
    private final MatchMode matchMode;

    private final Inventory inventory;

    public ArenaSelectorView(ModeSelectorView modeSelectorView, List<ArenaConfiguration> selectArenas, MatchMode mode) {
        this.modeSelectorView = modeSelectorView;
        this.selectArenas = selectArenas;
        this.matchMode = mode;

        final int rows = (int) Math.ceil(Math.max(selectArenas.size() + 1, 1) / 9.);
        this.inventory = Bukkit.createInventory(this, rows * 9, modeSelectorView.getPlugin().getLocale()
                .get("arena.selector.title").toString());
        load();
    }

    private void load() {
        final LocaleController locale = modeSelectorView.getPlugin().getLocale();
        for (int i = 0; i < selectArenas.size(); i++) {
            final ArenaConfiguration arena = selectArenas.get(i);

            final Map<String, String> variables = new HashMap<>();
            variables.put("arena", arena.name);

            final ItemStack icon = arena.icon.buildFromVariables(locale, variables);
            inventory.setItem(i, icon);
        }

        final ItemPlaceholder randomIconPlaceholder = new ItemPlaceholder(Material.NETHER_STAR, (short) 0, 0,
                "${arena.selector.arena.title}", "${arena.selector.arena.desc}");
        final Map<String, String> variables = new HashMap<>();
        variables.put("arena", "Random");
        inventory.setItem(inventory.getSize() - 1, randomIconPlaceholder.buildFromVariables(locale, variables));
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}
