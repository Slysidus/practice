package com.oxymore.practice.configuration.ui;

import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import lombok.AllArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class ViewConfiguration {
    public final int rows;
    public final String title;
    public final List<ViewPlaceholder> placeholders;

    public static class Deserialize implements Deserializer<ViewConfiguration> {
        @Override
        public ViewConfiguration deserialize(Parser parser, ConfigurationSection configuration, String _path)
                throws DeserializeException {
            final int rows = configuration.getInt("rows", 3);
            final String title = configuration.getString("title", "");

            final List<ViewPlaceholder> placeholders = new ArrayList<>();
            for (Map<?, ?> sectionMap : configuration.getMapList("layout")) {
                final ConfigurationSection viewSection = new YamlConfiguration().createSection("view", sectionMap);
                try {
                    placeholders.add(parser.parse(ViewPlaceholder.class, viewSection));
                } catch (Deserializer.DeserializeException e) {
                    throw e.parent("could not parse layout");
                }
            }

            return new ViewConfiguration(rows, title, placeholders);
        }
    }
}
