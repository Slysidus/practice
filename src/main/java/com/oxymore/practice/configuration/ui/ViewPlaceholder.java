package com.oxymore.practice.configuration.ui;

import com.oxymore.practice.configuration.condition.Condition;
import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import lombok.AllArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;

@AllArgsConstructor
public class ViewPlaceholder {
    public final int slot;
    public final Condition condition;
    public final String action;
    public final ItemPlaceholder itemPlaceholder;

    public static class Deserialize implements Deserializer<ViewPlaceholder> {
        @Override
        public ViewPlaceholder deserialize(Parser parser, ConfigurationSection configuration, String _path)
                throws DeserializeException {
            if (!configuration.contains("slot")) {
                throw new DeserializeException("slot is missing");
            }
            if (!configuration.isInt("slot")) {
                throw new DeserializeException("slot cannot be parsed as number");
            }
            final int slot = configuration.getInt("slot");

            final Condition condition = parser.parse(Condition.class, configuration, "if");
            final String action = configuration.getString("action");
            final ItemPlaceholder itemPlaceholder;
            try {
                itemPlaceholder = parser.parse(ItemPlaceholder.class, configuration);
            } catch (DeserializeException e) {
                throw e.parent("could not parse item placeholder");
            }
            return new ViewPlaceholder(slot, condition != null ? condition : v -> true, action, itemPlaceholder);
        }
    }
}
