package com.oxymore.practice.configuration;

import com.oxymore.practice.configuration.condition.Condition;
import com.oxymore.practice.configuration.match.*;
import com.oxymore.practice.configuration.misc.ItemConfiguration;
import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import com.oxymore.practice.configuration.ui.ItemPlaceholder;
import com.oxymore.practice.configuration.ui.ViewConfiguration;
import com.oxymore.practice.configuration.ui.ViewPlaceholder;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class ConfigurationReader implements Parser {
    private final Map<Class<?>, Deserializer<?>> parserMap;

    public ConfigurationReader() {
        this.parserMap = new HashMap<>();
        addDeserializer(Condition.class, new Condition.Deserialize());

        addDeserializer(ItemPlaceholder.class, new ItemPlaceholder.Deserialize());
        addDeserializer(ItemConfiguration.class, new ItemConfiguration.Deserialize());
        addDeserializer(ViewPlaceholder.class, new ViewPlaceholder.Deserialize());
        addDeserializer(ViewConfiguration.class, new ViewConfiguration.Deserialize());

        addDeserializer(GeneralArenasConfiguration.class, new GeneralArenasConfiguration.Deserialize());
        addDeserializer(ArenaConfiguration.class, new ArenaConfiguration.Deserialize());
        addDeserializer(MatchMode.class, new MatchMode.Deserialize());
        addDeserializer(Kit.class, new Kit.Deserialize());
        addDeserializer(EloSearchConfiguration.class, new EloSearchConfiguration.Deserialize());
    }

    // READ

    public PracticeConfiguration readConfiguration(Configuration configuration,
                                                   Configuration arenasConfiguration,
                                                   Configuration viewsConfiguration,
                                                   Configuration matchesConfiguration)
            throws Deserializer.DeserializeException {
        final String mongoUrl = configuration.getString("mongodb-url");

        final int endTeleportTime = configuration.getInt("end-teleport-time", 3);
        final boolean hideConnectMessages = configuration.getBoolean("hide-connect-messages", true);
        final boolean pingCommandEnabled = configuration.getBoolean("ping-command-enabled", true);

        final GeneralArenasConfiguration arenaConfiguration;
        try {
            arenaConfiguration = parse(GeneralArenasConfiguration.class, configuration.getConfigurationSection("arenas"));
        } catch (Deserializer.DeserializeException e) {
            throw e.parent("could not parse arena configuration");
        }

        final Map<String, ViewConfiguration> views = new HashMap<>();
        for (String viewName : viewsConfiguration.getKeys(false)) {
            try {
                views.put(viewName, parse(ViewConfiguration.class, viewsConfiguration.getConfigurationSection(viewName)));
            } catch (Deserializer.DeserializeException e) {
                throw e.parent("could not parse view configuration", viewName);
            }
        }

        final List<MatchMode> matchModes = new ArrayList<>();
        for (ConfigurationSection section : readKeyList(matchesConfiguration)) {
            try {
                matchModes.add(parse(MatchMode.class, section));
            } catch (Deserializer.DeserializeException e) {
                throw e.parent("could not parse mode", section.getName());
            }
        }

        final List<ArenaConfiguration> arenas = new ArrayList<>();
        for (ConfigurationSection section : readKeyList(arenasConfiguration)) {
            try {
                arenas.add(parse(ArenaConfiguration.class, section));
            } catch (Deserializer.DeserializeException e) {
                throw e.parent("could not parse arena", section.getName());
            }
        }

        final EloSearchConfiguration eloSearchConfiguration = parse(EloSearchConfiguration.class, configuration.getConfigurationSection("elo-search"));
        return new PracticeConfiguration(mongoUrl, endTeleportTime, hideConnectMessages, pingCommandEnabled, views,
                eloSearchConfiguration, arenaConfiguration, arenas, matchModes);
    }

    // UTILS

    private List<ConfigurationSection> readKeyList(ConfigurationSection configuration) {
        return configuration.getKeys(false).stream()
                .map(configuration::getConfigurationSection)
                .collect(Collectors.toList());
    }

    // PARSE

    private <T> void addDeserializer(Class<T> cls, Deserializer<T> deserializer) {
        parserMap.put(cls, deserializer);
    }

    @Override
    public <T> T parse(Class<T> cls, ConfigurationSection configuration, String path) throws Deserializer.DeserializeException {
        final Deserializer<T> deserializer = (Deserializer<T>) parserMap.get(cls);
        if (deserializer == null) {
            throw new Deserializer.DeserializeException("no deserializer found for type '", cls.getCanonicalName(), "'");
        }
        return deserializer.deserialize(this, configuration, path);
    }
}
