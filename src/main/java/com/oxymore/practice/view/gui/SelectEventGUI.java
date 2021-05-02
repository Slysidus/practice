package com.oxymore.practice.view.gui;

import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.controller.MatchingController;
import com.oxymore.practice.match.MatchType;
import com.oxymore.practice.match.party.Party;
import com.oxymore.practice.view.ViewContext;
import com.oxymore.practice.view.match.ModeSelectorView;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SelectEventGUI extends GUIView {
    public SelectEventGUI(ViewConfiguration viewConfiguration) {
        super(viewConfiguration);
    }

    @Override
    public void show(Player player, ViewContext viewContext) {
        super.show(player, viewContext);
        player.openInventory(setupInventory(viewContext));
    }

    @Override
    public void performAction(Practice plugin, Player player, String action) {
        switch (action) {
            case "split":
            case "ffa": {
                player.closeInventory();
                final MatchType matchType = action.equals("split") ? MatchType.SPLIT : MatchType.FFA;
                final ModeSelectorView view = new ModeSelectorView(plugin, new ArrayList<>(plugin.getConfiguration().matchModes),
                        matchType, (player1, matchType1, matchMode, arena) -> {
                    final Party party = plugin.getPartyController().getParty(player1);
                    if (party == null || !party.getLeader().equals(player1)) {
                        return;
                    }

                    if (party.getPlayers().size() < 2) {
                        plugin.getLocale().get("party.event.not-enough-players")
                                .send(player);
                        return;
                    }

                    final List<Player> team1, team2;
                    if (matchType1 == MatchType.SPLIT) {
                        final int playersInTeam = party.getPlayers().size() / 2;
                        team1 = party.getPlayers().stream()
                                .limit(playersInTeam)
                                .collect(Collectors.toList());
                        team2 = party.getPlayers().stream()
                                .skip(playersInTeam)
                                .collect(Collectors.toList());
                    } else {
                        team1 = new ArrayList<>(party.getPlayers());
                        team2 = null;
                    }
                    try {
                        plugin.getMatchingController().createMatch(matchMode, matchType1, arena, team1, team2);
                    } catch (MatchingController.MatchCreationException e) {
                        plugin.getLocale().get(e.getMessage())
                                .send(player);
                    }
                });
                view.load();
                view.open(player);
                break;
            }
        }
    }
}
