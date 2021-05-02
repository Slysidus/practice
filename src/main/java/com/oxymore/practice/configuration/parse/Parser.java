package com.oxymore.practice.configuration.parse;

import org.bukkit.configuration.ConfigurationSection;

public interface Parser {
    <T> T parse(Class<T> cls, ConfigurationSection configuration, String path) throws Deserializer.DeserializeException;

    default <T> T parse(Class<T> cls, ConfigurationSection configuration) throws Deserializer.DeserializeException {
        return parse(cls, configuration, null);
    }
}
