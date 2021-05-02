package com.oxymore.practice.match.party;

import com.oxymore.practice.LocaleController;
import lombok.Data;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Data
public class Party {
    private Player leader;
    private final List<Player> members;
    private boolean open;

    public Party(Player leader) {
        this.leader = leader;
        this.members = new ArrayList<>();
        this.open = false;
    }

    public List<Player> getPlayers() {
        final List<Player> players = new ArrayList<>(members);
        players.add(leader);
        return players;
    }

    public void broadcast(LocaleController.MessageContext messageCtx) {
        messageCtx.send(getPlayers());
    }
}
