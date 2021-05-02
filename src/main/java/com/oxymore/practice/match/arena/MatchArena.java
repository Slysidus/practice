package com.oxymore.practice.match.arena;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BlockVector;

@Data
public class MatchArena {
    private final Arena arena;
    private final Location initialLocation;

    public Location relativeLoc(BlockVector relVector, World world) {
        if (relVector == null) {
            return null;
        }
        return relVector.clone()
                .add(initialLocation.toVector())
                .toLocation(world);
    }
}
