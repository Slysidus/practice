package com.oxymore.practice.controller;

import com.mongodb.MongoConfigurationException;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.documents.EloDocument;
import com.oxymore.practice.documents.KitDocument;
import com.oxymore.practice.documents.PlayerDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public final class DatabaseController {
    private final Practice plugin;

    private final MongoClient mongoClient;
    private final Database database;

    private final Map<UUID, PlayerDocument> playerCache;
    private final Map<UUID, Integer> eloCache;

    public DatabaseController(Practice plugin) throws ControllerInitException {
        this.plugin = plugin;

        Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
        Logger.getLogger("org.bson").setLevel(Level.SEVERE);
        try {
            this.mongoClient = MongoClients.create(plugin.getConfiguration().mongoDBConnectionUrl);
        } catch (MongoConfigurationException e) {
            throw new ControllerInitException("database", "Unable to connect to MongoDB. Check your connection url.");
        }

        final CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(com.mongodb.MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        final MongoDatabase mongo = mongoClient.getDatabase("practice")
                .withCodecRegistry(pojoCodecRegistry);

        final MongoCollection<KitDocument> kits = mongo.getCollection("kits", KitDocument.class);
        final MongoCollection<PlayerDocument> players = mongo.getCollection("players", PlayerDocument.class);
        final MongoCollection<EloDocument> elo = mongo.getCollection("elo", EloDocument.class);

        this.database = new Database(plugin, mongo, kits, players, elo);
        this.playerCache = new ConcurrentHashMap<>();
        this.eloCache = new ConcurrentHashMap<>();
    }

    public void queryCacheUpdate(OfflinePlayer player, boolean moveAsync) {
        final AsyncExecutor op = db -> {
            final Bson query = Filters.eq("playerId", player.getUniqueId());
            final PlayerDocument playerDocument = db.players.find(query).first();
            final int elo = db.getAverageElo(query);
            playerCache.put(player.getUniqueId(), playerDocument);
            eloCache.put(player.getUniqueId(), elo);
        };
        if (moveAsync) {
            async(op);
        } else {
            op.execute(database);
        }
    }

    public void removeFromCache(Player player) {
        playerCache.remove(player.getUniqueId());
        eloCache.remove(player.getUniqueId());
    }

    @RequiredArgsConstructor
    public static class Database {
        private final Practice plugin;
        public final MongoDatabase mongo;

        public final MongoCollection<KitDocument> kits;
        public final MongoCollection<PlayerDocument> players;
        public final MongoCollection<EloDocument> elo;

        public void sync(Runnable runnable) {
            plugin.getServer().getScheduler().runTask(plugin, runnable);
        }

        public void syncIfOnline(OfflinePlayer target, Runnable runnable) {
            if (target == null) {
                return;
            }
            sync(() -> {
                if (target.isOnline()) {
                    runnable.run();
                }
            });
        }

        public int getAverageElo(Bson query) {
            final Document eloDoc = mongo.getCollection("elo").aggregate(Arrays.asList(
                    Aggregates.match(query),
                    Aggregates.group("_id", new BsonField("averageElo", new BsonDocument("$avg", new BsonString("$elo"))))
            )).first();
            int elo = eloDoc != null ? eloDoc.getDouble("averageElo").intValue() : 0;
            if (elo == 0) {
                elo = 1000;
            }
            return elo;
        }

        public Collection<LocaleController.ExpansionElement> fetchTop(Bson filter) {
            if (filter == null) {
                filter = new Document();
            }
            final Collection<LocaleController.ExpansionElement> entries = new ArrayList<>();
            final AggregateIterable<Document> aggregate = mongo.getCollection("elo").aggregate(Arrays.asList(
                    Aggregates.match(filter),
                    Aggregates.group("$playerId", Accumulators.avg("averageElo", "$elo")),
                    Aggregates.sort(Sorts.descending("averageElo")),
                    Aggregates.limit(10)
            ));
            int j = 0;
            for (Document eloDocument : aggregate) {
                final int rank = ++j;
                final String playerName = Bukkit.getOfflinePlayer((UUID) eloDocument.get("_id")).getName();
                final int elo = eloDocument.getDouble("averageElo").intValue();
                entries.add(e -> e
                        .var("rank", String.valueOf(rank))
                        .var("player", playerName)
                        .var("elo", String.valueOf(elo)));
            }
            return entries;
        }

        public void saveKit(KitDocument kitDocument) {
            final Bson filter = kitDocument.getFilter();
            if (kits.find(filter).first() != null) {
                kits.updateOne(filter, Updates.combine(
                        Updates.set("name", kitDocument.getName()),
                        Updates.set("kit", kitDocument.getKit())
                ));
            } else {
                kits.insertOne(kitDocument);
            }
        }

        public void deleteKit(KitDocument kitDocument) {
            kits.deleteOne(kitDocument.getFilter());
        }
    }

    public void async(AsyncExecutor asyncExecutor) {
        if (asyncExecutor == null) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> asyncExecutor.execute(database));
    }

    public interface AsyncExecutor {
        void execute(Database database);
    }
}
