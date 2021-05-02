package com.oxymore.practice.view.gui;

import com.google.common.base.Preconditions;
import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.Kit;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.configuration.misc.ItemConfiguration;
import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.documents.KitDocument;
import com.oxymore.practice.view.ViewContext;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitEditorGUI extends GUIView {
    public KitEditorGUI(ViewConfiguration viewConfiguration) {
        super(viewConfiguration);
    }

    @Override
    public void show(Player player, ViewContext viewContext) {
        super.show(player, viewContext);

        final KitEditorData data = (KitEditorData) viewContext.getData();

        final Map<String, String> variables = new HashMap<>();
        variables.put("name", data.kitDocument.getDisplayName());

        final Inventory inventory = setupInventory(ViewContext.builder()
                .locale(viewContext.getLocale())
                .variables(new LocaleController.LocaleVariables(variables, null))
                .build());

        final int offset = (viewConfiguration.rows - 4) * 9;
        for (ItemConfiguration additionalItem : data.matchMode.additionalItems) {
            final int firstEmpty = inventory.firstEmpty();
            if (firstEmpty < 0 || firstEmpty >= offset) {
                continue;
            }
            inventory.setItem(firstEmpty, additionalItem.build());
        }

        final Inventory tempInv = Bukkit.createInventory(null, 4 * 9);
        data.kitDocument.getKit().apply(tempInv, false);
        for (int i = 0; i < tempInv.getSize(); i++) {
            // show hotbar on bottom
            final int slot = offset + (i / 9 == 0 ? 3 * 9 + i : i - 9);
            inventory.setItem(slot, tempInv.getItem(i));
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
            case "rename": {
                final KitDocument kitDocument = ((KitEditorData) playerContexts.get(player.getUniqueId()).getData()).kitDocument;
                player.closeInventory();
                plugin.getKitRenaming().put(player, kitDocument);
                plugin.getLocale().get("kit.rename.prompt")
                        .var("name", kitDocument.getDisplayName())
                        .var("slot", String.valueOf(kitDocument.getSlot()))
                        .send(player);
                break;
            }
            case "delete": {
                final KitDocument kitDocument = ((KitEditorData) playerContexts.get(player.getUniqueId()).getData()).kitDocument;
                player.closeInventory();
                plugin.getDatabaseController().async(db -> {
                    db.deleteKit(kitDocument);
                    plugin.getLocale().get("kit.deleted")
                            .var("name", kitDocument.getDisplayName())
                            .var("slot", String.valueOf(kitDocument.getSlot()))
                            .send(player);
                });
                break;
            }
            case "save":
            case "save-and-close": {
                final Inventory inventory = player.getOpenInventory().getTopInventory();
                Preconditions.checkNotNull(inventory);

                final List<Kit.PositionalKitItem> items = new ArrayList<>();
                final int offset = (viewConfiguration.rows - 4) * 9;
                for (int i = 0; i < 4 * 9; i++) {
                    final ItemStack itemStack = inventory.getItem(offset + i);
                    if (itemStack != null) {
                        final int slot = i / 9 == 3 ? i - 3 * 9 : i + 9; // revert slot swapping for hotbar pos
                        final Map<String, Integer> enchants = new HashMap<>();
                        itemStack.getItemMeta().getEnchants().forEach((enchant, level) -> enchants.put(enchant.getName(), level));
                        items.add(new Kit.PositionalKitItem(new ItemConfiguration(itemStack.getType().name(), itemStack.getDurability(),
                                itemStack.getAmount(), enchants), slot));
                    }
                }

                final Kit kit = new Kit(items, new ArrayList<>());
                final KitDocument kitDocument = ((KitEditorData) playerContexts.get(player.getUniqueId()).getData()).kitDocument;

                // leave armor in kits
                for (Kit.PositionalKitItem positionalItem : kitDocument.getKit().getPositionalItems()) {
                    if (positionalItem.getSlot() >= 4 * 9) {
                        items.add(positionalItem);
                    }
                }

                kitDocument.setKit(kit);

                plugin.getDatabaseController().async(db -> {
                    db.saveKit(kitDocument);
                    plugin.getLocale().get("kit.saved")
                            .var("name", kitDocument.getDisplayName())
                            .send(player);
                });
                if (action.endsWith("-close")) {
                    player.closeInventory();
                }
                break;
            }
        }
    }

    @Data
    public static class KitEditorData {
        private final MatchMode matchMode;
        private final KitDocument kitDocument;
    }
}
