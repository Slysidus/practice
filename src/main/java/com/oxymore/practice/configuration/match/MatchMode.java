package com.oxymore.practice.configuration.match;

import com.oxymore.practice.configuration.misc.ItemConfiguration;
import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import com.oxymore.practice.configuration.ui.ItemPlaceholder;
import com.oxymore.practice.match.MatchType;
import lombok.AllArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.material.MaterialData;

import java.util.*;

@AllArgsConstructor
public class MatchMode {
    public final String id;
    public final String name;

    public final boolean terrainMutable;
    public final boolean regeneration;
    public final boolean fluidKills;
    public final int noDamageTicks;
    public final int enderpearlCooldown;

    public final Set<MatchType> supportTypes;
    public final boolean supportsDuel;
    public final List<String> arenas;

    public final MaterialData neutralIcon;
    public final ItemPlaceholder unrankedIcon;
    public final ItemPlaceholder rankedIcon;

    public final Kit defaultKit;
    public final boolean allowCustomKits;
    public final List<ItemConfiguration> additionalItems;

    @SuppressWarnings("deprecation")
    public static class Deserialize implements Deserializer<MatchMode> {
        @Override
        public MatchMode deserialize(Parser parser, ConfigurationSection configuration, String _path)
                throws DeserializeException {
            final String id = configuration.getName();
            final String name = configuration.getString("name");
            if (name == null) {
                throw new DeserializeException("missing name");
            }

            final boolean canEdit = configuration.getBoolean("terrain-mutable");
            final boolean regeneration = configuration.getBoolean("regeneration", true);
            final boolean fluidKills = configuration.getBoolean("fluid-kills");
            final int damageTicks = configuration.getInt("no-damage-ticks", -1);
            final int enderpearlCooldown = configuration.getInt("enderpearl-cooldown", 15);

            final ItemPlaceholder unrankedIcon;
            try {
                ConfigurationSection unrankedSection = configuration.getConfigurationSection("unranked");
                if (unrankedSection == null) {
                    unrankedSection = configuration.createSection("unranked");
                }
                unrankedSection.addDefault("title", "${modes.unranked.templates.title}");
                unrankedSection.addDefault("description", "${modes.unranked.templates.desc}");
                unrankedSection.addDefault("type", configuration.getString("type", ""));
                unrankedSection.addDefault("type-data", configuration.getInt("type-data", 0));
                unrankedIcon = parser.parse(ItemPlaceholder.class, unrankedSection);
            } catch (DeserializeException e) {
                throw e.parent("could not parse item unranked placeholder");
            }

            final ItemPlaceholder rankedIcon;
            try {
                ConfigurationSection rankedSection = configuration.getConfigurationSection("ranked");
                if (rankedSection == null) {
                    rankedSection = configuration.createSection("ranked");
                }
                rankedSection.addDefault("title", "${modes.ranked.templates.title}");
                rankedSection.addDefault("description", "${modes.ranked.templates.desc}");
                rankedSection.addDefault("type", configuration.getString("type", ""));
                rankedSection.addDefault("type-data", configuration.getInt("type-data", 0));
                rankedIcon = parser.parse(ItemPlaceholder.class, rankedSection);
            } catch (DeserializeException e) {
                throw e.parent("could not parse item ranked placeholder");
            }

            final MaterialData neutralIcon = new MaterialData(rankedIcon.getType(), (byte) rankedIcon.getDamage());

            final boolean supportsSingle = configuration.getBoolean("support.single", true);
            final boolean supportsDouble = configuration.getBoolean("support.double", true);
            final boolean supportsUnranked = configuration.getBoolean("support.unranked", true);
            final boolean supportsRanked = configuration.getBoolean("support.ranked", true);
            final boolean supportsDuel = configuration.getBoolean("support.duel", true);

            final Set<MatchType> supportTypes = new HashSet<>();
            for (MatchType matchType : MatchType.values()) {
                if (!matchType.isJoinable()) {
                    continue;
                }

                if (matchType.isSingle() && !supportsSingle) {
                    continue;
                }
                if (matchType.isDouble() && !supportsDouble) {
                    continue;
                }
                if (matchType.isUnranked() && !supportsUnranked) {
                    continue;
                }
                if (matchType.isRanked() && !supportsRanked) {
                    continue;
                }
                supportTypes.add(matchType);
            }

            // make sure arena names are distinct yet queryable with index
            final List<String> arenas = new ArrayList<>(new HashSet<>(configuration.getStringList("arenas")));

            final Kit defaultKit = parser.parse(Kit.class, configuration, "default-kit");
            final boolean allowCustomKits = configuration.getBoolean("allow-custom-kits", true);

            final List<ItemConfiguration> additionalItems = new ArrayList<>();
            for (Map<?, ?> sectionMap : configuration.getMapList("additional-items")) {
                final ConfigurationSection itemSection = new YamlConfiguration().createSection("additional-items", sectionMap);
                try {
                    additionalItems.add(parser.parse(ItemConfiguration.class, itemSection));
                } catch (Deserializer.DeserializeException e) {
                    throw e.parent("could not parse additional item entry");
                }
            }

            return new MatchMode(id, name,
                    canEdit, regeneration, fluidKills,
                    damageTicks, enderpearlCooldown,
                    supportTypes, supportsDuel,
                    arenas,
                    neutralIcon, unrankedIcon, rankedIcon,
                    defaultKit, allowCustomKits, additionalItems
            );
        }
    }
}
