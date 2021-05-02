package com.oxymore.practice.configuration.match;

import com.oxymore.practice.configuration.misc.ItemConfiguration;
import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.configuration.parse.Parser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Kit {
    private List<PositionalKitItem> positionalItems;
    private List<FillingKitItem> fillingKitItems;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PositionalKitItem {
        private ItemConfiguration item;
        private int slot;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FillingKitItem {
        private ItemConfiguration item;
        private int slots;
    }

    public void apply(Inventory inventory, boolean bypassLimits) {
        for (PositionalKitItem pos : positionalItems) {
            if (bypassLimits || pos.slot < inventory.getSize()) {
                inventory.setItem(pos.slot, pos.item.build());
            }
        }
        for (FillingKitItem fill : fillingKitItems) {
            final ItemStack itemStack = fill.getItem().build();
            for (int i = 0; i < fill.slots; i++) {
                inventory.addItem(itemStack);
            }
        }
    }

    public static class Deserialize implements Deserializer<Kit> {
        @Override
        public Kit deserialize(Parser parser, ConfigurationSection configuration, String path)
                throws DeserializeException {
            final List<PositionalKitItem> positionalItems = new ArrayList<>();
            final List<FillingKitItem> fillingKitItems = new ArrayList<>();

            if (configuration == null) {
                return new Kit(positionalItems, fillingKitItems);
            }

            for (Map<?, ?> sectionMap : configuration.getMapList(path)) {
                final ConfigurationSection itemSection = new YamlConfiguration().createSection(path, sectionMap);
                try {
                    final ItemConfiguration itemConfiguration = parser.parse(ItemConfiguration.class, itemSection);
                    if (itemSection.contains("slot")) {
                        final int slot = itemSection.getInt("slot");
                        positionalItems.add(new PositionalKitItem(itemConfiguration, slot));
                    } else {
                        final int slots = itemSection.getInt("fill", 1);
                        fillingKitItems.add(new FillingKitItem(itemConfiguration, slots));
                    }
                } catch (Deserializer.DeserializeException e) {
                    throw e.parent("could not parse kit entry");
                }
            }

            return new Kit(positionalItems, fillingKitItems);
        }
    }
}
