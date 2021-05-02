package fr.vinetos.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Vinetos
 */
public class VinetosReflection {

    // Get a class
    public static Class<?> getClass(String classname) {
        try {
            String version = VinetosReflection.getNmsVersion();
            String path = classname.replace("{nms}", "net.minecraft.server." + version)
                    .replace("{nm}", "net.minecraft." + version)
                    .replace("{cb}", "org.bukkit.craftbukkit." + version);
            return Class.forName(path);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    // Get a net.minecraft.server version
    public static String getNmsVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }

    // Get a nms player
    public static Object getNmsPlayer(Player p) throws Exception {
        Method getHandle = p.getClass().getMethod("getHandle");
        return getHandle.invoke(p);
    }

    // get a field
    public static Field getField(Class<?> clazz, String fieldName) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return field;
    }

    // Set a field value
    public static void setFieldValue(Object instance, Field field, Object value) {
        field.setAccessible(true);
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // Get player connection
    public static Object getPlayerConnection(Player player) throws Exception {
        Object nmsPlayer = VinetosReflection.getNmsPlayer(player);
        return nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
    }

    // Send packet to a connection player
    public static void sendPacket(Object connection, Object packet) throws Exception {
        connection.getClass().getMethod("sendPacket", VinetosReflection.getClass("{nms}.Packet")).invoke(connection, packet);
    }

    // Send packet to a list of player
    public static void sendPacket(Collection<? extends Player> players, Object packet) {
        if (packet == null) {
            return;
        }
        try {
            for (Player p : players) {
                VinetosReflection.sendPacket(VinetosReflection.getPlayerConnection(p), packet);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // Send a packet to a player
    public static void sendPacket(Player p, Object packet) {
        ArrayList<Player> list = new ArrayList<>();
        list.add(p);
        VinetosReflection.sendPacket(list, packet);
    }
}