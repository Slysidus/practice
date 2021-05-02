package com.oxymore.practice.configuration.misc;

import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemConfiguration {
    public String typeStr;
    public short damage;
    public int amount;
    public Map<String, Integer> enchants;

    @BsonIgnore
    public ItemStack build() {
        final ItemStack itemStack = new ItemStack(Material.getMaterial(typeStr), amount, damage);
        final ItemMeta itemMeta = itemStack.getItemMeta();

        if (enchants != null) {
            enchants.forEach((enchant, level) -> itemMeta.addEnchant(Enchantment.getByName(enchant), Math.max(1, level), true));
        }

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static class Deserialize implements Deserializer<ItemConfiguration> {
        @Override
        public ItemConfiguration deserialize(Parser _parser, ConfigurationSection itemSection, String _path)
                throws DeserializeException {
            final String typeStr = itemSection.getString("type");
            if (typeStr == null) {
                throw new DeserializeException("missing type");
            }
            final Material type = Material.getMaterial(typeStr.toUpperCase());
            if (type == null) {
                throw new DeserializeException("type '", typeStr, "' is not recognized");
            }

            final short damage;
            if (itemSection.contains("damage")) {
                damage = (short) itemSection.getInt("damage");
            } else if (itemSection.contains("type-data")) {
                damage = (short) itemSection.getInt("type-data");
            } else {
                damage = 0;
            }

            if (itemSection.contains("amount") && !itemSection.isInt("amount")) {
                throw new DeserializeException("amount cannot be parsed as number");
            }
            final int amount = itemSection.getInt("amount", 1);

            final Map<String, Integer> enchants = new HashMap<>();
            if (itemSection.isList("enchants")) {
                for (Map<?, ?> map : itemSection.getMapList("enchants")) {
                    final ConfigurationSection enchantSection = new YamlConfiguration()
                            .createSection("enchants", map);
                    final String enchantStr = enchantSection.getString("enchant");
                    if (enchantStr == null) {
                        throw new DeserializeException("an enchant is missing in enchant list");
                    }
                    final Enchantment enchantment = Enchantment.getByName(enchantStr);
                    if (enchantment == null) {
                        throw new DeserializeException("enchant '", enchantStr, "' is not recognized");
                    }
                    enchants.put(enchantment.getName(), enchantSection.getInt("level", 1));
                }
            }
            return new ItemConfiguration(type.name(), damage, amount, enchants);
        }
    }
}
