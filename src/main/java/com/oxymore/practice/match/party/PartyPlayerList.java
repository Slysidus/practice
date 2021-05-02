package com.oxymore.practice.match.party;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;

import java.util.ArrayList;

@Data
@EqualsAndHashCode(callSuper = true)
public class PartyPlayerList extends ArrayList<Player> {
    private final Party party;
}
