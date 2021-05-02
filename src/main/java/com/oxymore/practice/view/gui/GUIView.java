package com.oxymore.practice.view.gui;

import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.view.AbstractView;
import com.oxymore.practice.view.ViewContext;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

@Getter
public abstract class GUIView extends AbstractView implements InventoryHolder {
    public GUIView(ViewConfiguration viewConfiguration) {
        super(viewConfiguration);
    }

    @Override
    public void show(Player player, ViewContext viewContext) {
        super.show(player, viewContext);
    }

    protected Inventory setupInventory(ViewContext viewContext) {
        final String title = viewContext != null
                ? viewContext.getLocale().applyCtx(viewConfiguration.title, viewContext.getVariables())
                : ChatColor.translateAlternateColorCodes('&', viewConfiguration.title);
        final Inventory inventory = Bukkit.createInventory(this, viewConfiguration.rows * 9, title);
        setupInventory(inventory, viewContext);
        return inventory;
    }

    @Override
    public void destroy(Player player) {
        super.destroy(player);
    }

    @Override
    public Inventory getInventory() {
        throw new IllegalStateException();
    }
}
