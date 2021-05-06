package com.oxymore.practice.listener;

import com.google.common.base.Preconditions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.ArenaConfiguration;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.configuration.ui.ViewPlaceholder;
import com.oxymore.practice.controller.ViewController;
import com.oxymore.practice.documents.KitDocument;
import com.oxymore.practice.view.AbstractView;
import com.oxymore.practice.view.ViewContext;
import com.oxymore.practice.view.gui.GUIView;
import com.oxymore.practice.view.gui.KitEditGUI;
import com.oxymore.practice.view.gui.KitEditorGUI;
import com.oxymore.practice.view.match.ArenaSelectorView;
import com.oxymore.practice.view.match.ModeSelectorView;
import com.oxymore.practice.view.misc.KitModeSelectorView;
import com.oxymore.practice.view.player.PlayerView;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ViewListener implements Listener {
    private final Practice plugin;
    private final ViewController viewController;

    private final Map<Player, MovingItem> movingItems;

    public ViewListener(Practice plugin) {
        this.plugin = plugin;
        this.viewController = plugin.getViewController();
        this.movingItems = new HashMap<>();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final PlayerView playerView = viewController.getPlayerPanel(player).getPlayerView();
        if (playerView == null) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            handleAction(player, playerView, player.getInventory().getHeldItemSlot());
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getWhoClicked() == null) {
            return;
        }

        final Player player = (Player) event.getWhoClicked();
        if (player.getInventory().equals(event.getClickedInventory())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, player::updateInventory, 10);
        }

        if (event.getClickedInventory().getHolder() != null) {
            final InventoryHolder inventoryHolder = event.getClickedInventory().getHolder();
            if (inventoryHolder instanceof GUIView) {
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                        || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
                        || event.getAction() == InventoryAction.HOTBAR_SWAP) {
                    return;
                }

                final GUIView guiView = viewController.getPlayerPanel(player).getGUIView();
                Preconditions.checkNotNull(guiView);
                if (guiView instanceof KitEditorGUI) {
                    final int editFrom = (guiView.getViewConfiguration().rows - 4) * 9;
                    final int slot = event.getSlot();
                    final boolean infiniteSource = slot < editFrom && !guiView.getViewPlaceholders().containsKey(slot);
                    if (infiniteSource || slot >= editFrom) {
                        if (infiniteSource) {
                            event.setCursor(event.getCurrentItem());
                        } else {
                            event.setCancelled(false);
                        }
                        if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                            movingItems.put(player, new MovingItem(event.getCurrentItem(), infiniteSource ? -1 : slot));
                        } else {
                            movingItems.remove(player);
                        }
                    } else if (movingItems.containsKey(player)) {
                        final String action = getAction(player, guiView, slot);
                        if (action != null && action.equals("destroy-item")) {
                            movingItems.remove(player);
                            event.setCursor(null);
                        }
                    } else {
                        handleAction(player, guiView, slot);
                    }
                } else {
                    handleAction(player, guiView, event.getSlot());
                }
            } else if (inventoryHolder instanceof ArenaSelectorView) {
                final int slot = event.getSlot();
                final ArenaSelectorView arenaSelectorView = (ArenaSelectorView) inventoryHolder;
                final ModeSelectorView modeSelectorView = arenaSelectorView.getModeSelectorView();
                if (slot < arenaSelectorView.getSelectArenas().size()) {
                    player.closeInventory();
                    final ArenaConfiguration arena = arenaSelectorView.getSelectArenas().get(slot);
                    modeSelectorView.getCallback().onModeSelect(player, modeSelectorView.getMatchType(),
                            arenaSelectorView.getMatchMode(), arena.name);
                } else if (slot == event.getClickedInventory().getSize() - 1) {
                    player.closeInventory();
                    modeSelectorView.getCallback().onModeSelect(player, modeSelectorView.getMatchType(),
                            arenaSelectorView.getMatchMode(), null);
                }
            } else if (inventoryHolder instanceof ModeSelectorView) {
                final int slot = event.getSlot();
                final ModeSelectorView modeSelectorView = (ModeSelectorView) inventoryHolder;
                if (slot < modeSelectorView.getSelectModes().size()) {
                    player.closeInventory();
                    final MatchMode mode = modeSelectorView.getSelectModes().get(slot);
                    final List<ArenaConfiguration> arenas = plugin.getConfiguration().arenas.stream()
                            .filter(arena -> mode.arenas.contains(arena.name))
                            .collect(Collectors.toList());
                    if (arenas.size() > 0 && player.hasPermission("oxymore.practice.select-arena")) {
                        final ArenaSelectorView arenaSelectorView = new ArenaSelectorView(modeSelectorView, arenas, mode);
                        arenaSelectorView.open(player);
                    } else {
                        modeSelectorView.getCallback().onModeSelect(player, modeSelectorView.getMatchType(), mode, null);
                    }
                }
            } else if (inventoryHolder instanceof KitModeSelectorView) {
                final KitModeSelectorView selector = (KitModeSelectorView) inventoryHolder;
                final int slot = event.getSlot();
                if (slot < selector.getSelectModes().size()) {
                    final MatchMode mode = selector.getSelectModes().get(slot);
                    player.closeInventory();
                    plugin.getDatabaseController().async(db -> {
                        final KitDocument[] kits = new KitDocument[3];
                        final FindIterable<KitDocument> kitsIt = db.kits
                                .find(Filters.and(Filters.eq("owner", player.getUniqueId()), Filters.eq("modeId", mode.id)))
                                .limit(3);
                        for (KitDocument kitDocument : kitsIt) {
                            kits[kitDocument.getSlot()] = kitDocument;
                        }
                        final KitEditGUI.KitEditData kitEditData = new KitEditGUI.KitEditData(mode, kits);
                        db.syncIfOnline(player, () -> plugin.getViewPanel(player).setGUIView(ViewController.GUIViews.EDIT_KITS, ViewContext.builder()
                                .locale(plugin.getLocale())
                                .data(kitEditData)
                                .build()));
                    });
                }
            }
        } else if (event.getClickedInventory().equals(player.getInventory())) {
            final PlayerView playerView = viewController.getPlayerPanel(player).getPlayerView();
            if (playerView == null) {
                return;
            }
            handleAction(player, playerView, player.getInventory().getHeldItemSlot());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryDragItem(InventoryDragEvent event) {
        final Player player = (Player) event.getWhoClicked();
        if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null) {
            event.setCancelled(true);

            final Inventory inventory = player.getOpenInventory().getTopInventory();
            final InventoryHolder inventoryHolder = inventory.getHolder();
            if (inventoryHolder instanceof KitEditorGUI) {
                final GUIView guiView = (GUIView) inventoryHolder;
                Preconditions.checkNotNull(guiView);
                final int editFrom = (guiView.getViewConfiguration().rows - 4) * 9;
                final int editTo = inventory.getSize();
                final boolean forbiddenPositions = event.getRawSlots().stream()
                        .anyMatch(slot -> slot < editFrom || slot > editTo);
                if (!forbiddenPositions) {
                    event.setCancelled(false);
                    final MovingItem movingItem = movingItems.remove(player);
                    if (event.getCursor() != null && event.getCursor().getAmount() > 0) {
                        movingItems.put(player, new MovingItem(event.getCursor(), movingItem != null ? movingItem.slot : -1));
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        final Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof GUIView) {
            final Player player = (Player) event.getPlayer();
            viewController.getPlayerPanel(player).setGUIView(null, null);
            if (inventory.getHolder() instanceof KitEditorGUI) {
                final MovingItem movingItem = movingItems.remove(player);
                final InventoryView inventoryView = player.getOpenInventory();
                if (movingItem != null && inventoryView.getCursor() != null) {
                    Preconditions.checkArgument(movingItem.itemStack.equals(inventoryView.getCursor()));
                    inventoryView.setCursor(null);
                    if (movingItem.slot != -1) {
                        inventoryView.setItem(movingItem.slot, movingItem.itemStack);
                    } // else destroy
                }
            }
        }
    }

    private String getAction(Player player, AbstractView view, int slot) {
        if (!view.getViewPlaceholders().containsKey(slot)) {
            return null;
        }
        final ViewPlaceholder viewPlaceholder = view.getViewPlaceholders().get(slot)
                .getFirstMatching(view.getPlayerContexts().get(player.getUniqueId()));
        return viewPlaceholder != null ? viewPlaceholder.action : null;
    }

    private void handleAction(Player player, AbstractView view, int slot) {
        final String action = getAction(player, view, slot);
        if (action != null) {
            view.performAction(plugin, player, action);
        }
    }

    @Data
    private static class MovingItem {
        public final ItemStack itemStack;
        public final int slot;
    }
}
