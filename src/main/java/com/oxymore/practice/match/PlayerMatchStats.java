package com.oxymore.practice.match;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

@ToString
@EqualsAndHashCode
public class PlayerMatchStats {
    public int hits;
    public int combo;
    public int longestCombo;
    public int potions;
    public int kills;
    public int enderpearlCooldown;
    public boolean dead;

    public UUID snapshotId;
}
