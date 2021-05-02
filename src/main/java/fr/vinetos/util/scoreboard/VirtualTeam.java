package fr.vinetos.util.scoreboard;

import fr.vinetos.util.VinetosReflection;
import net.minecraft.server.v1_7_R4.PacketPlayOutScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

/**
 * Api for use teams with packet (version 1.7.10)
 * All field are verified and they works for 1.7.10 versions.
 * This class use Reflection from https://gist.github.com/Vinetos/6f497519f1b6465e833ac52006e9205b
 * <p>
 * To create a Team just call {@link #create()}.
 * To delete a Team just call {@link #remove()}.
 * N.B: You need to send the team of all new player. In PlayerJoinEvent for example, just add {@link #rebuildTeamPacket(Player)}.
 *
 * @author Vinetos
 */
public class VirtualTeam {
    // Factories
    private static final Class PACKET_CLASS = VinetosReflection.getClass("{nms}.PacketPlayOutScoreboardTeam");
    private PacketPlayOutScoreboardTeam p = null;

    // This fields haves to be updated for other version that 1.7.10 (v1_7_R4)
    private static final Field TEAM_NAME = VinetosReflection.getField(VirtualTeam.PACKET_CLASS, "a");
    private static final Field DISPLAY_NAME = VinetosReflection.getField(VirtualTeam.PACKET_CLASS, "b");
    private static final Field PREFIX = VinetosReflection.getField(VirtualTeam.PACKET_CLASS, "c");
    private static final Field SUFFIX = VinetosReflection.getField(VirtualTeam.PACKET_CLASS, "d");
    private static final Field MEMBERS = VinetosReflection.getField(VirtualTeam.PACKET_CLASS, "e");
    private static final Field TEAM_MODE = VinetosReflection.getField(VirtualTeam.PACKET_CLASS, "f");
    private static final Field OPTIONS = VinetosReflection.getField(VirtualTeam.PACKET_CLASS, "g");

    // Defines attributes
    private static Set<VirtualTeam> teams = new HashSet<>();
    private final String name;
    private String displayName = "";
    private String suffix = "";
    private String prefix = "";
    private Set<String> players = new HashSet<>();
    private List<Player> viewers = null;
    private DyeColor teamColor;
    private boolean created = false;

    // Options of the team
    private boolean friendlyFire = true;
    private boolean seeFriendlyInvisibles = false;

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name The name of the team (can't be changed after)
     */
    public VirtualTeam(String name) {
        this(name, (List<Player>) null);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name      The name of the team (can't be changed after)
     * @param teamColor The {@link DyeColor} of the team (to make a GUI for example)
     */
    public VirtualTeam(String name, DyeColor teamColor) {
        this(name, null, teamColor);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name    The name of the team (can't be changed after)
     * @param viewers The players who can see the team . Set to <code>null</code> for all connected players
     */
    public VirtualTeam(String name, List<Player> viewers) {
        this(name, "", name, "", new HashSet<>(), viewers);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name      The name of the team (can't be changed after)
     * @param viewers   The players who can see the team . Set to <code>null</code> for all connected players
     * @param teamColor The {@link DyeColor} of the team (to make a GUI for example)
     */
    public VirtualTeam(String name, List<Player> viewers, DyeColor teamColor) {
        this(name, "", name, "", new HashSet<>(), viewers, teamColor);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name        The name of the team (can't be changed after)
     * @param displayName The display name of the team (on the right scoreboard for example)
     */
    public VirtualTeam(String name, String displayName) {
        this(name, "", displayName, "", new HashSet<>(), (List<Player>) null);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name        The name of the team (can't be changed after)
     * @param suffix      The suffix of the team (after the player's name)
     * @param displayName The display name of the team (on the right scoreboard for example)
     * @param prefix      The prefix of the team (before the player's name)
     */
    public VirtualTeam(String name, String suffix, String displayName, String prefix) {
        this(name, suffix, displayName, prefix, new HashSet<>(), (List<Player>) null);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name      The name of the team (can't be changed after)
     * @param suffix    The suffix of the team (after the player's name)
     * @param prefix    The prefix of the team (before the player's name)
     * @param teamColor The {@link DyeColor} of the team (to make a GUI for example)
     */
    public VirtualTeam(String name, String suffix, String prefix, DyeColor teamColor) {
        this(name, suffix, name, prefix, new HashSet<>(), null, teamColor);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name        The name of the team (can't be changed after)
     * @param suffix      The suffix of the team (after the player's name)
     * @param displayName The display name of the team (on the right scoreboard for example)
     * @param prefix      The prefix of the team (before the player's name)
     * @param teamColor   The {@link DyeColor} of the team (to make a GUI for example)
     */
    public VirtualTeam(String name, String suffix, String displayName, String prefix, DyeColor teamColor) {
        this(name, suffix, displayName, prefix, new HashSet<>(), null, teamColor);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name        The name of the team (can't be changed after)
     * @param suffix      The suffix of the team (after the player's name)
     * @param displayName The display name of the team (on the right scoreboard for example)
     * @param prefix      The prefix of the team (before the player's name)
     * @param players     The players into the team
     */
    public VirtualTeam(String name, String suffix, String displayName, String prefix, Set<String> players) {
        this(name, suffix, displayName, prefix, players, (List<Player>) null);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name        The name of the team (can't be changed after)
     * @param suffix      The suffix of the team (after the player's name)
     * @param displayName The display name of the team (on the right scoreboard for example)
     * @param prefix      The prefix of the team (before the player's name)
     * @param players     The players into the team
     * @param teamColor   The {@link DyeColor} of the team (to make a GUI for example)
     */
    public VirtualTeam(String name, String suffix, String displayName, String prefix, Set<String> players, DyeColor teamColor) {
        this(name, suffix, displayName, prefix, players, null, teamColor);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name        The name of the team (can't be changed after)
     * @param suffix      The suffix of the team (after the player's name)
     * @param displayName The display name of the team (on the right scoreboard for example)
     * @param prefix      The prefix of the team (before the player's name)
     * @param viewers     The players who can see the team . Set to <code>null</code> for all connected players
     */
    public VirtualTeam(String name, String suffix, String displayName, String prefix, List<Player> viewers) {
        this(name, suffix, displayName, prefix, new HashSet<>(), viewers);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name        The name of the team (can't be changed after)
     * @param suffix      The suffix of the team (after the player's name)
     * @param displayName The display name of the team (on the right scoreboard for example)
     * @param prefix      The prefix of the team (before the player's name)
     * @param players     The players into the team
     * @param viewers     The players who can see the team . Set to <code>null</code> for all connected players
     */
    public VirtualTeam(String name, String suffix, String displayName, String prefix, Set<String> players, List<Player> viewers) {
        this(name, suffix, displayName, prefix, players, viewers, null);
    }

    /**
     * Create a team with packets. Call {@link #create()} to send the to viewers players
     *
     * @param name        The name of the team (can't be changed after)
     * @param suffix      The suffix of the team (after the player's name)
     * @param displayName The display name of the team (on the right scoreboard for example)
     * @param prefix      The prefix of the team (before the player's name)
     * @param players     The players into the team
     * @param viewers     The players who can see the team . Set to <code>null</code> for all connected players
     * @param teamColor   The {@link DyeColor} of the team (to make a GUI for example)
     */
    public VirtualTeam(String name, String suffix, String displayName, String prefix, Set<String> players, List<Player> viewers, DyeColor teamColor) {
        this.name = name;
        this.suffix = suffix;
        this.displayName = displayName;
        this.prefix = prefix;
        this.players = players;
        this.viewers = viewers;
        this.teamColor = teamColor;
    }

    /**
     * Get all created teams
     *
     * @return A {@link Set} of {@link VirtualTeam}
     */
    public static Set<VirtualTeam> getRegisterTeams() {
        return Collections.unmodifiableSet(VirtualTeam.teams);
    }

    /**
     * Get the team for a player
     *
     * @param player who is in a team
     * @return The {@link VirtualTeam} of the player
     */
    public static VirtualTeam getTeamOfPlayer(Player player) {
        for (VirtualTeam team : VirtualTeam.getRegisterTeams()) {
            if (team.hasPlayer(player)) {
                return team;
            }
        }
        return null;
    }

    /**
     * Send the team packet for the targeted players
     */
    public void create() {
        if (created) {
            update();
            return;
        }

        // Sends the team packet
        VinetosReflection.sendPacket(getViewers(), constructDefaultPacket(TeamMode.CREATE));

        // Send players (have to be one by one)
        getPlayers().forEach(playerName -> VinetosReflection.sendPacket(getViewers(), constructPlayerTeamPacket(TeamMode.ADD_PLAYER, playerName)));
        created = true;
    }

    /**
     * Send the team packet for the targeted players
     *
     * @param player The player who will have the team created
     */
    public void create(Player player) {
        if (isViewer(player)) {
            return;
        }
        // Sends the team packet
        VinetosReflection.sendPacket(player, constructDefaultPacket(TeamMode.CREATE));

        // Send players (have to be one by one)
        getPlayers().forEach(playerName -> VinetosReflection.sendPacket(getViewers(), constructPlayerTeamPacket(TeamMode.ADD_PLAYER, playerName)));
    }

    /**
     * Update the parameters of the team
     */
    public void update() {
        if (!created) {
            return;
        }

        // Send the team packet
        VinetosReflection.sendPacket(getViewers(), constructDefaultPacket(TeamMode.UPDATE));
    }

    /**
     * Update the parameters of the team for a player
     *
     * @param player The player who will have the team updated
     */
    public void update(Player player) {
        if (!isViewer(player)) {
            return;
        }
        // Send the team packet
        VinetosReflection.sendPacket(player, constructDefaultPacket(TeamMode.UPDATE));
    }

    /**
     * Add or remove a player
     */
    public void updatePlayer(TeamMode mode, String playerName) {
        if (!created) {
            return;
        }

        // Send the new player
        VinetosReflection.sendPacket(getViewers(), constructPlayerTeamPacket(mode, playerName));
    }

    /**
     * Remove the team for the all players. You can call {@link #create()} to re-make the team.
     */
    public void remove() {
        if (!created) {
            return;
        }

        // Remove players (have to be one by one)
        //getPlayers().forEach(playerName -> constructPlayerTeamPacket(TeamMode.REMOVE_PLAYER, playerName));

        // Delete the team
        VinetosReflection.sendPacket(getViewers(), constructDefaultPacket(TeamMode.REMOVE));

        created = false;
    }

    /**
     * /**
     * Remove the team for a player. You can call {@link #create(Player)} to re-make the team.
     *
     * @param player The player who will have the team removed
     */
    public void remove(Player player) {
        if (!isViewer(player)) {
            return;
        }
        // Remove players (have to be one by one)
        getPlayers().forEach(playerName -> constructPlayerTeamPacket(TeamMode.REMOVE_PLAYER, playerName));

        // Delete the team
        VinetosReflection.sendPacket(player, constructDefaultPacket(TeamMode.REMOVE));
    }

    /**
     * Delete the team for ever ! After, you can't all {@link #create()}. Be safe !
     */
    public void delete() {
        if (created) {
            remove();
        }

        VirtualTeam.teams.remove(this);
    }

    /**
     * Construct the generic packet
     *
     * @param mode Mode of the team
     * @return the packet built
     */
    private Object constructDefaultPacket(TeamMode mode) {
        if (mode != TeamMode.CREATE && mode != TeamMode.REMOVE && mode != TeamMode.UPDATE) {
            return null;
        }
        try {
            Object teamPacket = VirtualTeam.PACKET_CLASS.newInstance();
            VinetosReflection.setFieldValue(teamPacket, VirtualTeam.TEAM_NAME, this.name);
            VinetosReflection.setFieldValue(teamPacket, VirtualTeam.TEAM_MODE, mode.getMode());
            if (mode == TeamMode.REMOVE) {
                return teamPacket;
            }

            VinetosReflection.setFieldValue(teamPacket, VirtualTeam.DISPLAY_NAME, this.displayName);
            VinetosReflection.setFieldValue(teamPacket, VirtualTeam.PREFIX, this.prefix);
            VinetosReflection.setFieldValue(teamPacket, VirtualTeam.SUFFIX, this.suffix);
            VinetosReflection.setFieldValue(teamPacket, VirtualTeam.MEMBERS, this.players);
            VinetosReflection.setFieldValue(teamPacket, VirtualTeam.OPTIONS, packOptionData());
            return teamPacket;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create a packet for adding/removing a player
     *
     * @param mode       mode of packet (Adding or removing)
     * @param playerName The player who be updated
     * @return the packet built
     */
    private Object constructPlayerTeamPacket(TeamMode mode, String playerName) {
        if (mode != TeamMode.ADD_PLAYER && mode != TeamMode.REMOVE_PLAYER) {
            return null;
        }
        try {
            Object teamPacket = VirtualTeam.PACKET_CLASS.newInstance();
            VinetosReflection.setFieldValue(teamPacket, VirtualTeam.TEAM_NAME, this.name);
            VinetosReflection.setFieldValue(teamPacket, VirtualTeam.MEMBERS, Collections.singleton(playerName));
            VinetosReflection.setFieldValue(teamPacket, VirtualTeam.TEAM_MODE, mode.getMode());
            return teamPacket;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Generate the int for options
     *
     * @return the value of options
     */
    private int packOptionData() {
        int option = 0;

        if (this.allowFriendlyFire()) {
            option |= 1;
        }
        if (this.canSeeFriendlyInvisibles()) {
            option |= 2;
        }
        return option;
    }

    /**
     * Rebuild and resend the team for a specific player
     *
     * @param p the player who need to get the team
     */
    public void rebuildTeamPacket(Player p) {
        VinetosReflection.sendPacket(p, constructDefaultPacket(TeamMode.CREATE));

        // Send players (have to be one by one)
        getPlayers().forEach(playerName -> VinetosReflection.sendPacket(p, constructPlayerTeamPacket(TeamMode.ADD_PLAYER, playerName)));
    }

    /**
     * Check if the team is created
     *
     * @return <code>true</code> if the team is created
     */
    public boolean isCreated() {
        return this.created;
    }

    /**
     * Get the name of the team
     *
     * @return The name of the team
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the display name of the team
     *
     * @return the display name of the team
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Set the display name
     *
     * @param displayName The new display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        update();
    }

    /**
     * Get the prefix of a team
     *
     * @return the prefix of the team
     */
    public String getPrefix() {
        return this.prefix;
    }

    /**
     * Set the prefix of a team
     *
     * @param prefix the new prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        update();
    }

    /**
     * Get the suffix of a team
     *
     * @return the suffix of the team
     */
    public String getSuffix() {
        return this.suffix;
    }

    /**
     * Set the suffix of a team
     *
     * @param suffix the new suffix
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
        update();
    }

    /**
     * Check if the team allow the friendly fire
     *
     * @return <code>true</code> if the team allow the friendly fire
     */
    public boolean allowFriendlyFire() {
        return this.friendlyFire;
    }

    /**
     * Set if the team allow the friendly fire
     *
     * @param friendlyFire the new value of friendly fire
     */
    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
        update();
    }

    /**
     * Check if the team can see her members invisibles
     *
     * @return <code>true</code> if can see her members invisibles
     */
    public boolean canSeeFriendlyInvisibles() {
        return this.seeFriendlyInvisibles;
    }

    /**
     * Set if the team can see her members invisibles
     *
     * @param seeFriendlyInvisibles the new value of see members invisibles
     */
    public void setSeeFriendlyInvisibles(boolean seeFriendlyInvisibles) {
        this.seeFriendlyInvisibles = seeFriendlyInvisibles;
        update();
    }

    /**
     * Add a player to the team
     *
     * @param player the player who be added
     */
    public void addPlayer(Player player) {
        addPlayer(player.getName());
    }

    /**
     * Add a player to the team
     *
     * @param playerName the name of player who be added
     */
    public void addPlayer(String playerName) {
        this.players.add(playerName);

        // Send update
        updatePlayer(TeamMode.ADD_PLAYER, playerName);
    }

    /**
     * Check if a player is in a team
     *
     * @param player the player who be checked
     * @return <code>true</code> if the player is in the team
     */
    public boolean hasPlayer(Player player) {
        return this.players.contains(player.getName());
    }

    /**
     * Check if a player is in a team
     *
     * @param playerName the name of player who be checked
     * @return <code>true</code> if the player is in the team
     */
    public boolean hasPlayer(String playerName) {
        return this.players.contains(playerName);
    }

    /**
     * Remove a player from a team
     *
     * @param player the player who be removed
     */
    public void removePlayer(Player player) {
        removePlayer(player.getName());
    }

    /**
     * Remove a player from a team
     *
     * @param playerName the name player who be removed
     */
    public void removePlayer(String playerName) {
        this.players.remove(playerName);

        // Send update
        updatePlayer(TeamMode.REMOVE_PLAYER, playerName);
    }

    /**
     * Get the color of a team
     *
     * @return THe {@link DyeColor} of the team
     */
    public DyeColor getTeamColor() {
        return this.teamColor;
    }

    /**
     * Set the color for a team
     *
     * @param teamColor the new color of the team
     */
    public void setTeamColor(DyeColor teamColor) {
        this.teamColor = teamColor;
    }

    /**
     * Add a player who can see the team
     *
     * @param player who can see the team
     */
    public void addViewer(Player player) {
        if (getViewers().contains(player)) {
            return;
        }
        this.viewers.add(player);
        // Send the team to the new viewers
        create(player);
    }

    /**
     * Check if a player see the team
     *
     * @param player the player who be testes
     * @return <code>true</code> if the player see the team
     */
    public boolean isViewer(Player player) {
        return getViewers().contains(player);
    }

    /**
     * Remove a player who can see the team
     *
     * @param player who can see the team
     */
    public void removeViewer(Player player) {
        if (!getViewers().contains(player)) {
            return;
        }
        this.viewers.remove(player);
        // Send the team to the new viewers
        remove(player);
    }

    /**
     * Get players who can see the team
     *
     * @return A {@link List} of <code>? extends </code>{@link Player}
     */
    public List<Player> getViewers() {
        if (viewers == null) {
            this.viewers = new ArrayList<>();
            Stream.of(Bukkit.getOnlinePlayers()).forEach(player -> this.viewers.add((Player) player));
        }
        return Collections.unmodifiableList(viewers);
    }

    /**
     * Get player in the team
     *
     * @return A {@link Collection} of {@link String}
     */
    public Set<String> getPlayers() {
        if (this.players == null) {
            this.players = new HashSet<>();
        }
        return Collections.unmodifiableSet(this.players);
    }

    /**
     * The mode of the team (when the client receive the packet, what action it have to do)
     */
    public enum TeamMode {

        CREATE(0),
        REMOVE(1),
        UPDATE(2),
        ADD_PLAYER(3),
        REMOVE_PLAYER(4);

        private final int mode;

        TeamMode(final int mode) {
            this.mode = mode;
        }

        public final int getMode() {
            return this.mode;
        }
    }
}
