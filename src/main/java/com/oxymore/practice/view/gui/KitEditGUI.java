package com.oxymore.practice.view.gui;

import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.controller.ViewController;
import com.oxymore.practice.documents.KitDocument;
import com.oxymore.practice.view.ViewContext;
import lombok.Data;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitEditGUI extends GUIView {
    public KitEditGUI(ViewConfiguration viewConfiguration) {
        super(viewConfiguration);
    }

    @Override
    public void show(Player player, ViewContext viewContext) {
        super.show(player, viewContext);

        final KitEditData data = (KitEditData) viewContext.getData();

        final List<String> conditionValues = new ArrayList<>();
        final Map<String, String> variables = new HashMap<>();
        variables.put("mode", data.mode.name);
        variables.put("mode-id", data.mode.id);
        for (int i = 1; i < data.kits.length + 1; i++) {
            final KitDocument kit = data.kits[i - 1];
            if (kit != null) {
                variables.put("kit-" + i + "-name", kit.getDisplayName());
                conditionValues.add("kit-" + i + "-exists");
            } else {
                variables.put("kit-" + i + "-name", String.valueOf(i));
            }
        }

        final Inventory inventory = setupInventory(ViewContext.builder()
                .locale(viewContext.getLocale())
                .conditionValues(conditionValues)
                .variables(new LocaleController.LocaleVariables(variables, null))
                .build());
        player.openInventory(inventory);
    }

    @Override
    public void performAction(Practice plugin, Player player, String action) {
        if (action.startsWith("kit-")) {
            final int idx;
            try {
                idx = Integer.parseInt(action.substring(4)) - 1;
            } catch (NumberFormatException ignored) {
                return;
            }

            final KitEditData data = (KitEditData) playerContexts.get(player.getUniqueId()).getData();
            if (idx >= data.kits.length) {
                return;
            }

            KitDocument kitDocument = data.kits[idx];
            if (kitDocument == null) {
                kitDocument = new KitDocument(player.getUniqueId(), data.mode.id, idx, null, data.mode.defaultKit);
            }
            player.closeInventory();
            plugin.getViewPanel(player).setGUIView(ViewController.GUIViews.EDIT_KIT, ViewContext.builder()
                    .locale(plugin.getLocale())
                    .data(new KitEditorGUI.KitEditorData(data.mode, kitDocument))
                    .build());
        }
    }

    @Data
    public static class KitEditData {
        private final MatchMode mode;
        private final KitDocument[] kits;
    }
}
