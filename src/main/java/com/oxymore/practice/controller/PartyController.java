package com.oxymore.practice.controller;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.oxymore.practice.Practice;
import com.oxymore.practice.match.party.Party;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@Getter
public final class PartyController {
    private final Practice plugin;

    private final Map<Player, Party> parties;
    private final Multimap<Player, Party> invites;

    public PartyController(Practice plugin) throws ControllerInitException {
        this.plugin = plugin;
        this.parties = new HashMap<>();
        this.invites = ArrayListMultimap.create();
    }

    public Party getParty(Player player) {
        return parties.get(player);
    }

    public void partyHook(Player player) {
        Preconditions.checkNotNull(player);
        plugin.getViewController().updateSpawnView(player, true);
    }

    public void disbandParty(Party party) {
        Preconditions.checkNotNull(party);
        parties.values().removeIf(it -> it.equals(party));
        invites.values().removeIf(val -> val == null || val.equals(party));
        party.getPlayers().forEach(this::partyHook);
    }

    public void onDisconnect(Player player) {
        invites.removeAll(player);
        final Party party = parties.remove(player);
        if (party != null) {
            if (party.getLeader().equals(player)) {
                if (!party.getMembers().isEmpty()) {
                    party.setLeader(party.getMembers().get(0));
                    party.broadcast(plugin.getLocale().get("party.leader-quit")
                            .var("previous-leader", player.getName())
                            .var("new-leader", party.getLeader().getName()));
                } else {
                    disbandParty(party);
                }
            } else {
                party.getMembers().remove(player);
                party.broadcast(plugin.getLocale().get("party.quit")
                        .var("player", player.getName()));
            }
        }
    }
}
