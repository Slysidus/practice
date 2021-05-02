package com.oxymore.practice.configuration.ui;

import com.oxymore.practice.LocaleController;
import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import lombok.Data;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

@Data
public class ItemPlaceholder {
    private final Material type;
    private final short damage;
    private final int amount;

    private final String title;
    private final String description;

    public ItemStack build() {
        return build(ItemPlaceholder::def, ItemPlaceholder::def);
    }

    public ItemStack build(Function<String, String> titleAdapter, Function<String, String> descriptionAdapter) {
        final ItemStack itemStack = new ItemStack(type, amount, damage);
        final ItemMeta itemMeta = itemStack.getItemMeta();

        if (title != null) {
            itemMeta.setDisplayName(titleAdapter.apply(title));
        }
        if (description != null) {
            final String inlineLore = descriptionAdapter.apply(description);
            itemMeta.setLore(Arrays.asList(inlineLore.split("\\n")));
        }

        itemMeta.spigot().setUnbreakable(true);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack buildFromVariables(LocaleController locale, Map<String, String> variables) {
        return buildFromVariables(locale, new LocaleController.LocaleVariables(variables, null));
    }

    public ItemStack buildFromVariables(LocaleController locale, LocaleController.LocaleVariables variables) {
        return build(
                title -> locale.applyCtx(title, variables),
                desc -> locale.applyCtx(desc, variables)
        );
    }

    public static String def(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static class Deserialize implements Deserializer<ItemPlaceholder> {
        @Override
        public ItemPlaceholder deserialize(Parser _parser, ConfigurationSection configuration, String _path)
                throws DeserializeException {
            final String typeStr = configuration.getString("type");
            if (typeStr == null) {
                throw new DeserializeException("missing type");
            }
            final Material type = Material.getMaterial(typeStr.toUpperCase());
            if (type == null) {
                throw new DeserializeException("type '", typeStr, "' is not recognized");
            }

            final short damage;
            if (configuration.contains("damage")) {
                damage = (short) configuration.getInt("damage");
            } else if (configuration.contains("type-data")) {
                damage = (short) configuration.getInt("type-data");
            } else {
                damage = 0;
            }

            if (configuration.contains("amount") && !configuration.isInt("amount")) {
                throw new DeserializeException("amount cannot be parsed as number");
            }
            final int amount = configuration.getInt("amount", 1);

            final String title = configuration.getString("title");
            final String description = configuration.getString("description");

            return new ItemPlaceholder(type, damage, amount, title, description);
        }
    }
}
