package fr.vinetos.util.scoreboard;

import fr.vinetos.util.VinetosReflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.UUID;

/**
 * @author zyuiop
 * Updated and modified by Vinetos (for 1.7.10)
 * Reflection.java: https://gist.github.com/Vinetos/6f497519f1b6465e833ac52006e9205b
 * Team.java: https://gist.github.com/Vinetos/5f5066f190fd6c77b349c332bdb0d1d6
 */
public class ScoreboardSign {
    /**
     * Factories
     */
    private static final Class OBJECTIVE_PACKET_CLASS = VinetosReflection.getClass("{nms}.PacketPlayOutScoreboardObjective");
    private static final Field O_NAME = VinetosReflection.getField(ScoreboardSign.OBJECTIVE_PACKET_CLASS, "a");
    private static final Field O_VALUE = VinetosReflection.getField(ScoreboardSign.OBJECTIVE_PACKET_CLASS, "b");
    private static final Field O_MODE = VinetosReflection.getField(ScoreboardSign.OBJECTIVE_PACKET_CLASS, "c");

    private static final Class DISPLAY_OBJECTIVE_PACKET_CLASS = VinetosReflection.getClass("{nms}.PacketPlayOutScoreboardDisplayObjective");
    private static final Field DO_SLOT = VinetosReflection.getField(ScoreboardSign.DISPLAY_OBJECTIVE_PACKET_CLASS, "a");
    private static final Field DO_OBJ_NAME = VinetosReflection.getField(ScoreboardSign.DISPLAY_OBJECTIVE_PACKET_CLASS, "b");

    private static final Class SCORE_PACKET_CLASS = VinetosReflection.getClass("{nms}.PacketPlayOutScoreboardScore");
    private static final Field S_SCORE_NAME = VinetosReflection.getField(ScoreboardSign.SCORE_PACKET_CLASS, "a");
    private static final Field S_OBJ_NAME = VinetosReflection.getField(ScoreboardSign.SCORE_PACKET_CLASS, "b");
    private static final Field S_SCORE_INT = VinetosReflection.getField(ScoreboardSign.SCORE_PACKET_CLASS, "c");
    private static final Field S_ACTION = VinetosReflection.getField(ScoreboardSign.SCORE_PACKET_CLASS, "d");

    private final VirtualTeam[] lines = new VirtualTeam[15];
    private UUID playerUUID;
    private final String objectiveName;
    private ScoreboardSlot slot;
    private boolean created = false;

    /**
     * Create a scoreboard for a given player and using a specific objective name
     *
     * @param playerUUID    the player's uuid viewing the scoreboard sign
     * @param objectiveName the name of the scoreboard sign (displayed at the top of the scoreboard)
     * @param slot          the slot where the scoreboard will be displayed
     */
    public ScoreboardSign(UUID playerUUID, String objectiveName, ScoreboardSlot slot) {
        this.playerUUID = playerUUID;
        this.objectiveName = objectiveName;
        this.slot = slot;
    }

    /**
     * Send the initial creation packets for this scoreboard. Must be called at least once.
     */
    public void create() {
        if (created) {
            return;
        }
        // Send the scoreboard
        VinetosReflection.sendPacket(getPlayer(), createObjectivePacket(ScoreboardMode.CREATE, objectiveName));
        // Send the slot
        VinetosReflection.sendPacket(getPlayer(), setObjectiveSlot(slot));
        // Send lines
        int i = 0;
        while (i < lines.length) {
            sendLine(i++);
        }
        created = true;
    }

    /**
     * Send the packets to update this scoreboard. A scoreboard must be created using {@link ScoreboardSign#create()} in order
     * to be used
     */
    public void update() {
        if (!created) {
            return;
        }

        // Send the scoreboard's update
        VinetosReflection.sendPacket(getPlayer(), createObjectivePacket(ScoreboardMode.UPDATE, objectiveName));
    }


    /**
     * Send the packets to remove this scoreboard. A destroyed scoreboard sign must be recreated using {@link ScoreboardSign#create()} in order
     * to be used again
     */
    public void destroy() {
        if (!created) {
            return;
        }

        // Remove the scoreboard
        VinetosReflection.sendPacket(getPlayer(), createObjectivePacket(ScoreboardMode.REMOVE, null));

        // Remove teams
        for (VirtualTeam team : lines) {
            if (team != null && team.isCreated()) {
                team.remove();
            }
        }
        created = false;
    }

    /**
     * Change a scoreboard line and send the packets to the player. Can be called async.
     *
     * @param line  the number of the line (0 <= line < 15)
     * @param value the new value for the scoreboard line
     */
    public void setLine(int line, String value) {
        VirtualTeam team = getOrCreateTeam(line);
        String old = getValue(team);

        if (old != null && created) {
            removeLine(team, old);
        }

        setValue(team, value);
        sendLine(line);
    }

    /**
     * Remove a given scoreboard line
     *
     * @param line the line to remove
     */
    public void removeLine(int line) {
        VirtualTeam team = getOrCreateTeam(line);
        String old = getValue(team);
        if (!created) {
            return;
        }
        if (old != null) {
            removeLine(team, old);
        }

    }

    /**
     * Get the current value for a line
     *
     * @param line the line
     * @return the content of the line
     */
    public String getLine(int line) {
        if (line > 14 || line < 0) {
            return "";
        }
        return getValue(getOrCreateTeam(line));
    }

    private void sendLine(int line) {
        if (line > 14 || line < 0) {
            return;
        }
        if (!created) {
            return;
        }

        int score = (15 - line);
        VirtualTeam team = getOrCreateTeam(line);
        String lineValue = getValue(team);

        VinetosReflection.sendPacket(getPlayer(), sendScore(lineValue, score));
    }

    private void removeLine(VirtualTeam team, String scoreName) {
        try {
            team.removePlayer(team.getPlayers().iterator().next());
            VinetosReflection.sendPacket(getPlayer(), ScoreboardSign.SCORE_PACKET_CLASS.getDeclaredConstructor(String.class).newInstance(scoreName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VirtualTeam getOrCreateTeam(int line) {
        if (lines[line] == null) {
            lines[line] = new VirtualTeam("__fakeScore" + line, Collections.singletonList(getPlayer()));
            lines[line].create();
        }
        return lines[line];
    }

    private Player getPlayer() {
        final Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            throw new NullPointerException("The player of uuid " + playerUUID.toString() + " isn't online !");
        }
        return player;
    }

    private void setValue(VirtualTeam team, String value) {
        final int length = value.length();
        if (length <= 16) {
            team.setPrefix("");
            team.addPlayer(value);
            team.setSuffix("");
        } else if (value.length() <= 32) {
            team.setPrefix(value.substring(0, 16));
            team.addPlayer(value.substring(16));
            team.setSuffix("");
        } else if (value.length() <= 48) {
            team.setPrefix(value.substring(0, 16));
            team.addPlayer(value.substring(16, 32));
            team.setSuffix(value.substring(32));
        } else {
            throw new IllegalArgumentException("Too long value ! Max 48 characters, value was " + length + " !");
        }
    }

    private String getValue(VirtualTeam team) {
        if (!team.getPlayers().iterator().hasNext()) {
            return null;
        }
        return team.getPrefix() + team.getPlayers().iterator().next() + team.getSuffix();
    }

    private Object createObjectivePacket(ScoreboardMode mode, String displayName) {
        try {
            Object packet = ScoreboardSign.OBJECTIVE_PACKET_CLASS.newInstance();
            // Nom de l'objectif
            VinetosReflection.setFieldValue(packet, ScoreboardSign.O_NAME, getPlayer().getName());

            // Mode
            // 0 : créer
            // 1 : Supprimer
            // 2 : Mettre à jour
            VinetosReflection.setFieldValue(packet, ScoreboardSign.O_MODE, mode.getMode());

            if (mode != ScoreboardMode.REMOVE) {
                VinetosReflection.setFieldValue(packet, ScoreboardSign.O_VALUE, displayName);
            }

            return packet;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    private Object setObjectiveSlot(ScoreboardSlot slot) {
        try {
            Object packet = ScoreboardSign.DISPLAY_OBJECTIVE_PACKET_CLASS.newInstance();

            // Slot
            VinetosReflection.setFieldValue(packet, ScoreboardSign.DO_SLOT, slot.getMode());
            VinetosReflection.setFieldValue(packet, ScoreboardSign.DO_OBJ_NAME, getPlayer().getName());
            return packet;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Object sendScore(String line, int score) {
        try {
            Object packet = ScoreboardSign.SCORE_PACKET_CLASS.getDeclaredConstructor(String.class).newInstance(line);
            VinetosReflection.setFieldValue(packet, ScoreboardSign.S_OBJ_NAME, getPlayer().getName());
            VinetosReflection.setFieldValue(packet, ScoreboardSign.S_SCORE_INT, score);
            VinetosReflection.setFieldValue(packet, ScoreboardSign.S_ACTION, 0);
            return packet;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * The mode of the scoreboard (when the client receive the packet, what action it have to do)
     */
    public enum ScoreboardMode {
        CREATE(0),
        REMOVE(1),
        UPDATE(2);

        private final int mode;

        ScoreboardMode(final int mode) {
            this.mode = mode;
        }

        public final int getMode() {
            return this.mode;
        }
    }

    /**
     * The slot where the scoreboard will be displayed
     */
    public enum ScoreboardSlot {
        LIST(0),
        SIDEBAR(1),
        BELOW_NAME(2);

        private final int mode;

        ScoreboardSlot(final int mode) {
            this.mode = mode;
        }

        public final int getMode() {
            return this.mode;
        }

    }

    /**
     * The mode of the score
     */
    public enum ScoreMode {
        CREATE_OR_UPDATE(0),
        REMOVE(1);

        private final int mode;

        ScoreMode(final int mode) {
            this.mode = mode;
        }

        public final int getMode() {
            return this.mode;
        }
    }
}
