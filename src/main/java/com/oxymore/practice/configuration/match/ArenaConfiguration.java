package com.oxymore.practice.configuration.match;

import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import com.oxymore.practice.configuration.ui.ItemPlaceholder;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

@AllArgsConstructor
public class ArenaConfiguration {
    public final String name;
    public final ItemPlaceholder icon;
    public final String schematicFile;

    public final Material spawnBlockType;
    public final Material spawnBlockReplace;

    public final Material centerBlockType;
    public final Material centerBlockReplace;

    public static class Deserialize implements Deserializer<ArenaConfiguration> {
        @Override
        public ArenaConfiguration deserialize(Parser parser, ConfigurationSection configuration, String _path) throws DeserializeException {
            final String name = configuration.getName();

            final ItemPlaceholder icon;
            try {
                ConfigurationSection iconSection = configuration.getConfigurationSection("icon");
                if (iconSection == null) {
                    iconSection = configuration.createSection("icon");
                }
                iconSection.addDefault("title", "${arena.selector.arena.title}");
                iconSection.addDefault("description", "${arena.selector.arena.desc}");

                icon = parser.parse(ItemPlaceholder.class, iconSection);
            } catch (DeserializeException e) {
                throw e.parent("unable to parse icon");
            }

            if (!configuration.contains("schematic")) {
                throw new DeserializeException("schematic field missing");
            }
            final String schematicFile = configuration.getString("schematic");

            final String spawnBlockTypeStr = configuration.getString("spawn-block.type");
            if (spawnBlockTypeStr == null) {
                throw new DeserializeException("missing spawn block type");
            }
            final Material spawnBlockType = Material.getMaterial(spawnBlockTypeStr.toUpperCase());
            if (spawnBlockType == null) {
                throw new DeserializeException("spawn block type '", spawnBlockTypeStr, "' is not recognized");
            }

            final String spawnBlockReplaceStr = configuration.getString("spawn-block.replace", "GRASS");
            final Material spawnBlockReplace = Material.getMaterial(spawnBlockReplaceStr.toUpperCase());
            if (spawnBlockReplace == null) {
                throw new DeserializeException("spawn block replace type '", spawnBlockReplaceStr, "' is not recognized");
            }

            final String centerBlockTypeStr = configuration.getString("center-block.type", "BEACON");
            final Material centerBlockType = Material.getMaterial(centerBlockTypeStr.toUpperCase());
            if (centerBlockType == null) {
                throw new DeserializeException("center block type '", centerBlockTypeStr, "' is not recognized");
            }

            final String centerBlockReplaceStr = configuration.getString("center-block.replace", "AIR");
            final Material centerBlockReplace = Material.getMaterial(centerBlockReplaceStr.toUpperCase());
            if (centerBlockReplace == null) {
                throw new DeserializeException("center block replace type '", centerBlockReplaceStr, "' is not recognized");
            }
            return new ArenaConfiguration(name, icon, schematicFile, spawnBlockType, spawnBlockReplace, centerBlockType, centerBlockReplace);
        }
    }
}
