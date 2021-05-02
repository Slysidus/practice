package com.oxymore.practice.controller;

import com.google.common.base.Preconditions;
import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.match.party.Party;
import com.oxymore.practice.view.ViewContext;
import com.oxymore.practice.view.gui.*;
import com.oxymore.practice.view.player.DefaultView;
import com.oxymore.practice.view.player.PartyView;
import com.oxymore.practice.view.player.PlayerView;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;

public final class ViewController {
    private final Practice plugin;

    private final Map<PlayerViews, PlayerView> playerViews;
    private final Map<GUIViews, GUIView> guiViews;

    private final Map<UUID, ViewPanel> playerPanels;

    public ViewController(Practice plugin) throws ControllerInitException {
        this.plugin = plugin;

        final Map<String, ViewConfiguration> views = plugin.getConfiguration().views;
        final Map<PlayerViews, PlayerView> playerViews = new HashMap<>();
        for (PlayerViews entry : PlayerViews.values()) {
            if (!views.containsKey(entry.path)) {
                throw new ControllerInitException("views", "player view " + entry.path + " is missing from views.yml");
            }

            try {
                final PlayerView playerView = entry.viewClass.getConstructor(ViewConfiguration.class)
                        .newInstance(views.get(entry.path));
                playerViews.put(entry, playerView);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new ControllerInitException(e);
            }
        }
        this.playerViews = playerViews;

        final Map<GUIViews, GUIView> guiViews = new HashMap<>();
        for (GUIViews entry : GUIViews.values()) {
            if (!views.containsKey(entry.path)) {
                throw new ControllerInitException("views", "gui view " + entry.path + " is missing from views.yml");
            }

            try {
                final GUIView guiView = entry.viewClass.getConstructor(ViewConfiguration.class)
                        .newInstance(views.get(entry.path));
                guiViews.put(entry, guiView);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new ControllerInitException(e);
            }
        }
        this.guiViews = guiViews;

        this.playerPanels = new HashMap<>();
    }

    public void updateSpawnView(Player player, boolean refresh) {
        final Party party = plugin.getPartyController().getParty(player);
        final ViewController.PlayerViews viewEnum = party != null ?
                ViewController.PlayerViews.PARTY : ViewController.PlayerViews.DEFAULT;

        final Function<ViewContext, ViewContext> applyConditions = ctx -> ctx
                .updateCondition("leader", party != null && party.getLeader().equals(player))
                .updateCondition("in-queue", plugin.getMatchingController().isInQueue(player));

        if (refresh) {
            plugin.getDatabaseController().async(db -> {
                final Map<String, Collection<LocaleController.ExpansionElement>> expansions = new HashMap<>();
                expansions.put("entries", db.fetchTop(null));
                db.syncIfOnline(player, () -> getPlayerPanel(player).setPlayerView(viewEnum,
                        applyConditions.apply(ViewContext.builder()
                                .locale(plugin.getLocale())
                                .conditionValues(new ArrayList<>())
                                .variables(new LocaleController.LocaleVariables(null, expansions))
                                .build())));
            });
        } else if (plugin.getMatchingController().isSpawn(player)) {
            final PlayerView playerView = getPlayerPanel(player).getPlayerView();
            if (playerView == null) {
                updateSpawnView(player, true);
                return;
            }
            final ViewContext ctx = playerView.getPlayerContexts().get(player.getUniqueId());
            playerView.show(player, applyConditions.apply(ctx));
        }
    }

    // EVENT HOOKS

    public void joinPlayer(Player player) {
        quitPlayer(player);
        playerPanels.put(player.getUniqueId(), new ViewPanel(player));
    }

    public void quitPlayer(Player player) {
        final ViewPanel viewPanel = playerPanels.remove(player.getUniqueId());
        if (viewPanel != null) {
            viewPanel.destroy();
        }
    }

    // PANELS

    public ViewPanel getPlayerPanel(Player player) {
        final UUID uniqueId = player.getUniqueId();
        Preconditions.checkArgument(playerPanels.containsKey(uniqueId), "uninitialized player panel");
        return playerPanels.get(uniqueId);
    }

    @Getter
    @RequiredArgsConstructor
    public class ViewPanel {
        private final Player player;

        private PlayerView playerView;
        @Getter(AccessLevel.NONE)
        private GUIView guiView;

        public void setPlayerView(PlayerViews view, ViewContext viewContext) {
            if (playerView != null) {
                playerView.destroy(player);
            }
            if (view == null) {
                this.playerView = null;
                return;
            }
            this.playerView = ViewController.this.playerViews.get(view);
            playerView.show(player, viewContext);
        }

        public void setGUIView(GUIViews view, ViewContext viewContext) {
            if (guiView != null) {
                guiView.destroy(player);
            }
            if (view == null) {
                this.guiView = null;
                return;
            }
            this.guiView = ViewController.this.guiViews.get(view);
            guiView.show(player, viewContext);
        }

        public void destroy() {
            if (playerView != null) {
                playerView.destroy(player);
                this.playerView = null;
            }
            if (guiView != null) {
                player.closeInventory();
                guiView.destroy(player);
                this.guiView = null;
            }
        }

        public GUIView getGUIView() {
            return guiView;
        }
    }

    // VIEW TYPES

    @RequiredArgsConstructor
    public enum PlayerViews {
        DEFAULT("player_default", DefaultView.class),
        PARTY("player_party", PartyView.class),
        ;

        private final String path;
        private final Class<? extends PlayerView> viewClass;
    }

    @RequiredArgsConstructor
    public enum GUIViews {
        SELECT_AUX("gui_select-aux", SelectAuxGUI.class),
        SELECT_EVENT("gui_select-event", SelectEventGUI.class),
        EDIT_KITS("gui_edit-kits", KitEditGUI.class),
        EDIT_KIT("gui_edit-kit", KitEditorGUI.class),
        STATS("gui_stats", PlayerMatchStatsGUI.class),
        ;

        private final String path;
        private final Class<? extends GUIView> viewClass;
    }
}
