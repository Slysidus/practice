package com.oxymore.practice.match.arena;

import com.oxymore.practice.configuration.match.ArenaConfiguration;
import lombok.Data;
import org.bukkit.util.BlockVector;

import java.io.File;
import java.util.List;

@Data
public class Arena {
    private final ArenaConfiguration configuration;
    private final File schematicFile;
    private final BlockVector centerPoint;
    private final List<BlockVector> spawnLocations;
}
