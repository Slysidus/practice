package com.oxymore.practice.controller;

import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.ArenaConfiguration;
import com.oxymore.practice.match.arena.Arena;
import com.oxymore.practice.match.arena.MatchArena;
import com.oxymore.practice.util.SnailGrid;
import com.oxymore.practice.util.Vector2D;
import com.oxymore.practice.util.WorldEditUtil;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.*;
import org.bukkit.material.MaterialData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

@Getter
public final class ArenaController {
    private final Practice plugin;

    private final Map<String, Arena> arenas;

    private final World world;
    private final SnailGrid grid;
    private final Map<Vector2D, MatchArena> matchArenas;

    private final Stack<FreeArena> freeArenas;

    @SuppressWarnings("deprecation")
    public ArenaController(Practice plugin) throws ControllerInitException {
        this.plugin = plugin;

        final String worldName = "practice_arenas";
        Bukkit.unloadWorld(worldName, false);
        try {
            Files.walk((new File(plugin.getServer().getWorldContainer(), worldName)).toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {
        }

        final Map<String, Arena> arenas = new HashMap<>();
        for (ArenaConfiguration arenaConfiguration : plugin.getConfiguration().arenas) {
            final File schematicFile = new File(plugin.getDataFolder(), arenaConfiguration.schematicFile);
            if (!schematicFile.exists()) {
                throw new ControllerInitException("arena",
                        "schematic file '" + arenaConfiguration.schematicFile + "' (" + arenaConfiguration.name + ") does not exit");
            }

            final Arena arena = new Arena(arenaConfiguration, schematicFile,
                    WorldEditUtil.listSpawnPoints(schematicFile, new MaterialData(arenaConfiguration.centerBlockType, (byte) -1))
                            .stream().findAny().orElse(null),
                    WorldEditUtil.listSpawnPoints(schematicFile, new MaterialData(arenaConfiguration.spawnBlockType, (byte) -1))
            );
            arenas.put(arenaConfiguration.name, arena);
        }
        this.arenas = arenas;

        this.world = new WorldCreator(worldName)
                .type(WorldType.FLAT)
                .generateStructures(false)
                .createWorld();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                world.getChunkAt(i, j).load();
            }
        }

        this.grid = new SnailGrid(plugin.getConfiguration().arena.distance, 0, 0);
        this.matchArenas = new HashMap<>();
        this.freeArenas = new Stack<>();
    }

    public MatchArena makeMatchArena(Arena arena) {
        boolean needPasting = false;
        Vector2D position = null;
        if (!freeArenas.isEmpty()) {
            final FreeArena freeArena = freeArenas.stream()
                    .filter(free -> free.arena.equals(arena))
                    .findFirst().orElse(null);
            if (freeArena != null) {
                freeArenas.remove(freeArena);
                position = freeArena.position;
            }
        }
        if (position == null) {
            position = grid.next();
            needPasting = true;
        }

        final Location initialLocation = new Location(world, position.getX(), 100, position.getZ());
        final int chunkX = initialLocation.getChunk().getX();
        final int chunkZ = initialLocation.getChunk().getZ();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                world.getChunkAt(chunkX + i, chunkZ + j);
            }
        }

        if (needPasting) {
            final boolean result = WorldEditUtil.pasteSchematic(arena.getSchematicFile(), initialLocation);
            if (!result) {
                return null;
            }
        }

        return new MatchArena(arena, initialLocation);
    }

    public void releaseMatchArena(MatchArena matchArena) {
        Vector2D position = null;
        for (Map.Entry<Vector2D, MatchArena> entry : matchArenas.entrySet()) {
            if (entry.getValue().equals(matchArena)) {
                position = entry.getKey();
                break;
            }
        }
        if (position != null) {
            matchArenas.remove(position);
            freeArenas.push(new FreeArena(position, matchArena.getArena()));
        }
    }

    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class FreeArena {
        public final Vector2D position;
        public final Arena arena;
    }
}
