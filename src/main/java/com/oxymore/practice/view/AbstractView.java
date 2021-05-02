package com.oxymore.practice.view;

import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.configuration.ui.ViewPlaceholder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@Getter
public abstract class AbstractView implements View {
    protected final ViewConfiguration viewConfiguration;
    protected final Map<Integer, ViewPlaceholdersGroup> viewPlaceholders;

    protected final Map<UUID, ViewContext> playerContexts;

    public AbstractView(ViewConfiguration viewConfiguration) {
        this.viewConfiguration = viewConfiguration;
        this.viewPlaceholders = new HashMap<>();
        for (ViewPlaceholder placeholder : viewConfiguration.placeholders) {
            ViewPlaceholdersGroup group = viewPlaceholders.get(placeholder.slot);
            if (group == null) {
                group = new ViewPlaceholdersGroup();
                viewPlaceholders.put(placeholder.slot, group);
            }
            group.add(placeholder);
        }
        this.playerContexts = new HashMap<>();
    }

    @Override
    public void show(Player player, ViewContext viewContext) {
        playerContexts.put(player.getUniqueId(), viewContext);
    }

    @Override
    public void destroy(Player player) {
        playerContexts.remove(player.getUniqueId());
    }

    protected void setupInventory(Inventory inventory, ViewContext viewContext) {
        viewPlaceholders.forEach((slot, group) -> {
            final ViewPlaceholder viewPlaceholder = group.getFirstMatching(viewContext);

            final ItemStack itemStack;
            if (viewPlaceholder != null) {
                if (viewContext != null && viewContext.getVariables() != null) {
                    itemStack = viewPlaceholder.itemPlaceholder
                            .buildFromVariables(viewContext.getLocale(), viewContext.getVariables());
                } else {
                    itemStack = viewPlaceholder.itemPlaceholder.build();
                }
            } else {
                itemStack = null;
            }

            if (itemStack == null || !itemStack.equals(inventory.getItem(slot))) {
                inventory.setItem(slot, itemStack);
            }
        });
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ViewPlaceholdersGroup extends ArrayList<ViewPlaceholder> {
        public ViewPlaceholder getFirstMatching(ViewContext viewContext) {
            final Collection<String> values = viewContext != null && viewContext.getConditionValues() != null ?
                    viewContext.getConditionValues() : Collections.emptyList();
            return super.stream()
                    .filter(placeholder -> placeholder.condition.test(values))
                    .findFirst()
                    .orElse(null);
        }
    }
}
