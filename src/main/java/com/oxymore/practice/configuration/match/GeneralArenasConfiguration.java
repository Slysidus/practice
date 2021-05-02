package com.oxymore.practice.configuration.match;

import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import lombok.AllArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;

@AllArgsConstructor
public class GeneralArenasConfiguration {
    public final int bufferMinimalSize;
    public final int distance;

    public static class Deserialize implements Deserializer<GeneralArenasConfiguration> {
        @Override
        public GeneralArenasConfiguration deserialize(Parser _parser, ConfigurationSection configuration, String _path)
                throws DeserializeException {
            final int bufferMinimalSize = configuration.getInt("buffer-minimal-size", 0);
            final int distance = configuration.getInt("distance", 250);
            return new GeneralArenasConfiguration(bufferMinimalSize, distance);
        }
    }
}
