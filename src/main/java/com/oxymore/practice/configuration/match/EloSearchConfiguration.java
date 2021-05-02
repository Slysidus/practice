package com.oxymore.practice.configuration.match;

import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import lombok.AllArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;

@AllArgsConstructor
public class EloSearchConfiguration {
    public final int start;
    public final int increase;
    public final int increaseInterval;
    public final int stop;

    public static class Deserialize implements Deserializer<EloSearchConfiguration> {
        @Override
        public EloSearchConfiguration deserialize(Parser _parser, ConfigurationSection configuration, String _path)
                throws DeserializeException {
            final int start = configuration.getInt("start", 20);
            final int increase = configuration.getInt("increase", 10);
            final int increaseInterval = configuration.getInt("increase-interval", 5);
            final int stop = configuration.getInt("stop", 200);
            return new EloSearchConfiguration(start, increase, increaseInterval, stop);
        }
    }
}
