package com.oxymore.practice.configuration.parse;

import org.bukkit.configuration.ConfigurationSection;

public interface Deserializer<T> {
    T deserialize(Parser parser, ConfigurationSection configuration, String key) throws DeserializeException;

    class DeserializeException extends Exception {
        public DeserializeException(String... s) {
            super(String.join(" ", s));
        }

        public DeserializeException parent(String... s) {
            return new DeserializeException(String.join(" ", s), "| Reason:", getMessage());
        }
    }
}
