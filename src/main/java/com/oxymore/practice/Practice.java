package com.oxymore.practice;

import com.oxymore.practice.commands.*;
import com.oxymore.practice.configuration.ConfigurationReader;
import com.oxymore.practice.configuration.PracticeConfiguration;
import com.oxymore.practice.configuration.parse.Deserializer;
import com.oxymore.practice.controller.*;
import com.oxymore.practice.documents.KitDocument;
import com.oxymore.practice.listener.ChatListener;
import com.oxymore.practice.listener.ConnectionListener;
import com.oxymore.practice.listener.MatchListener;
import com.oxymore.practice.listener.ViewListener;
import com.oxymore.practice.view.match.MatchSelector;
import com.oxymore.practice.tasks.MatchLoop;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Getter
public final class Practice extends JavaPlugin {
    // config
    private PracticeConfiguration configuration;
    private LocaleController locale;

    // controllers
    private ArenaController arenaController;
    private DatabaseController databaseController;
    private ViewController viewController;
    private PartyController partyController;
    private MatchingController matchingController;
    private VisibilityController visibilityController;
    private ScoreboardController scoreboardController;

    // i don't know where this should be so here it is
    private Map<Player, KitDocument> kitRenaming;

    // INIT

    @Override
    public void onEnable() {
        final ConfigurationReader configurationReader = new ConfigurationReader();
        try {
            this.configuration = configurationReader.readConfiguration(
                    getConfig("config.yml"), getConfig("arenas.yml"),
                    getConfig("views.yml"), getConfig("modes.yml")
            );
        } catch (Deserializer.DeserializeException e) {
            initFailed("Unable to load configuration.\nError: " + e.getMessage());
            return;
        }
        this.locale = new LocaleController(getConfig("messages.yml"));

        // init controllers
        try {
            this.arenaController = new ArenaController(this);
            this.databaseController = new DatabaseController(this);
            this.viewController = new ViewController(this);
            this.partyController = new PartyController(this);
            this.matchingController = new MatchingController(this);
            this.visibilityController = new VisibilityController(this);
            this.scoreboardController = new ScoreboardController(this);
        } catch (ControllerInitException e) {
            initFailed(e.getMessage());
            return;
        }

        this.kitRenaming = new ConcurrentHashMap<>();

        // init selectors
        matchingController.getSelectors().values().forEach(MatchSelector::update);

        final PluginManager pluginManager = getServer().getPluginManager();

        // listeners
        final ConnectionListener connectionListener;
        pluginManager.registerEvents(connectionListener = new ConnectionListener(this), this);
        pluginManager.registerEvents(new MatchListener(this), this);
        pluginManager.registerEvents(new ViewListener(this), this);
        pluginManager.registerEvents(new ChatListener(this), this);

        // commands
        getCommand("duel").setExecutor(new DuelCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("spectate").setExecutor(new SpectateCommand(this));
        getCommand("elo").setExecutor(new EloCommand(this));
        getCommand("ping").setExecutor(new PingCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
        getCommand("matchstats").setExecutor(new MatchStatsCommand(this));
        getCommand("tournament").setExecutor(new TournamentCommand(this));
        getCommand("party").setExecutor(new PartyCommand(this));

        // tasks
        final BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.runTaskTimer(this, new MatchLoop(matchingController), 0, 20);

        // dev
        if (!getServer().getOnlinePlayers().isEmpty()) {
            getLogger().info("Reload detected, manually triggering player joins.");
            getServer().getOnlinePlayers()
                    .forEach(player -> connectionListener.onConnect(new PlayerJoinEvent(player, null)));
        }
    }

    private void initFailed(String str) {
        getLogger().severe(str);
        getLogger().severe("Practice plugin failed to initialize, disabling. Please check the details above.");
        getServer().getPluginManager().disablePlugin(this);
    }

    @Override
    public void onDisable() {
        anyhow(() -> Bukkit.getOnlinePlayers().forEach(scoreboardController::destroyScoreboard));
        anyhow(() -> databaseController.getMongoClient().close());
    }

    private interface Anyhow {
        void run() throws Throwable;
    }

    private void anyhow(Anyhow runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {
        }
    }

    // UTILS

    private Configuration getConfig(String fileName) {
        final Level initialLevel = getLogger().getLevel();
        getLogger().setLevel(Level.OFF);
        saveResource(fileName, false);
        getLogger().setLevel(initialLevel);
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), fileName));
    }

    // SHORTHANDS

    public ViewController.ViewPanel getViewPanel(Player player) {
        return viewController.getPlayerPanel(player);
    }
}

