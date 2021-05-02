package com.oxymore.practice.util;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.world.DataException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class WorldEditUtil {
    private WorldEditUtil() {
        throw new IllegalStateException();
    }

    @SuppressWarnings("deprecation")
    public static List<BlockVector> listSpawnPoints(File schematicFile, MaterialData expectedBlockData) {
        if (!schematicFile.exists()) {
            return null;
        }

        final SchematicFormat schematic = SchematicFormat.getFormat(schematicFile);
        final CuboidClipboard cuboidClipboard;
        try {
            cuboidClipboard = schematic.load(schematicFile);
        } catch (IOException | DataException e) {
            e.printStackTrace();
            return null;
        }

        final int expectedId = expectedBlockData.getItemTypeId();
        final byte expectedData = expectedBlockData.getData();

        final List<BlockVector> spawnPoints = new ArrayList<>();
        final int maxX = cuboidClipboard.getWidth(),
                maxY = cuboidClipboard.getHeight(),
                maxZ = cuboidClipboard.getLength();

        for (int x = 0; x < maxX; ++x) {
            for (int y = 0; y < maxY; ++y) {
                for (int z = 0; z < maxZ; ++z) {
                    final BaseBlock block = cuboidClipboard.getBlock(new Vector(x, y, z));
                    if (block == null) {
                        continue;
                    }

                    if (block.getId() == expectedId) {
                        if ((expectedData == -1) || (block.getData() == expectedData)) {
                            spawnPoints.add(new BlockVector(x, y, z));
                        }
                    }
                }
            }
        }
        return spawnPoints;
    }

    public static boolean pasteSchematic(File schematicFile, Location location) {
        if (!schematicFile.exists()) {
            return false;
        }

        final EditSession editSession = WorldEditUtil.createEditSession(location.getWorld());
        final SchematicFormat schematic = SchematicFormat.getFormat(schematicFile);
        final Vector origin = new Vector(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        try {
            schematic.load(schematicFile).paste(editSession, origin, false, false);
        } catch (MaxChangedBlocksException | IOException | DataException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static EditSession createEditSession(World world) {
        final WorldEdit worldEdit = WorldEdit.getInstance();
        return worldEdit.getEditSessionFactory().getEditSession(WorldEditUtil.toWEWorld(world),
                worldEdit.getConfiguration().maxChangeLimit);
    }

    private static com.sk89q.worldedit.world.World toWEWorld(World world) {
        return new BukkitWorld(world);
    }
}
