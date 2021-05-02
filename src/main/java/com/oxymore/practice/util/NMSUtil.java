package com.oxymore.practice.util;

import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.NBTTagList;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;

// no need for reflection, server is meant to be 1.7 forever I guess
public final class NMSUtil {
    private NMSUtil() {
        throw new IllegalStateException();
    }

    public static int getPing(Player player) {
        return ((CraftPlayer) player).getHandle().ping;
    }

    public static ItemStack setItemFlags(ItemStack itemStack, ItemFlag... flags) {
        final net.minecraft.server.v1_7_R4.ItemStack craftItemStack = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tag = craftItemStack.getTag();
        if (tag == null) {
            craftItemStack.setTag(new NBTTagCompound());
            tag = craftItemStack.getTag();
        }

        // FIXME: item bits do not work
        // maybe the server doesn't send them the raw tag to the client?
//        int hideBits = 0; // 1.8 HideFlags
        for (ItemFlag flag : new HashSet<>(Arrays.asList(flags))) {
            switch (flag) {
                case HIDE_ENCHANTS:
//                    hideBits |= 1;
                    if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasEnchants()) {
                        tag.set("ench", new NBTTagList());
                    }
                    break;
//                case HIDE_ATTRIBUTES:
//                    hideBits |= 2;
//                    break;
//                case HIDE_UNBREAKABLE:
//                    hideBits |= 4;
//                    break;
//                case HIDE_OTHERS:
//                    hideBits |= 32;
//                    break;
            }
        }
//        if (hideBits > 0) {
//            tag.setInt("HideFlags", hideBits);
//        }
//
        craftItemStack.setTag(tag);
        return CraftItemStack.asCraftMirror(craftItemStack);
    }

    // compat item flag for 1.7
    public enum ItemFlag {
        HIDE_ENCHANTS,
        HIDE_ATTRIBUTES,
        HIDE_UNBREAKABLE,
        HIDE_OTHERS,
    }
}
