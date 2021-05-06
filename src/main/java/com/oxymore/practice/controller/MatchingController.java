package com.oxymore.practice.controller;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.oxymore.practice.LocaleController;
import com.oxymore.practice.Practice;
import com.oxymore.practice.configuration.match.Kit;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.configuration.ui.ItemPlaceholder;
import com.oxymore.practice.documents.EloDocument;
import com.oxymore.practice.documents.KitDocument;
import com.oxymore.practice.match.*;
import com.oxymore.practice.match.arena.Arena;
import com.oxymore.practice.match.arena.MatchArena;
import com.oxymore.practice.match.party.Party;
import com.oxymore.practice.match.party.PartyPlayerList;
import com.oxymore.practice.match.queue.MatchQueue;
import com.oxymore.practice.match.queue.impl.MatchQueueEntry;
import com.oxymore.practice.match.queue.impl.RankedMatchQueue;
import com.oxymore.practice.match.queue.impl.RankedMatchQueueEntry;
import com.oxymore.practice.match.queue.impl.UnrankedMatchQueue;
import com.oxymore.practice.util.NMSUtil;
import com.oxymore.practice.view.gui.PlayerMatchStatsGUI;
import com.oxymore.practice.view.match.MatchSelector;
import com.oxymore.practice.view.match.ModeSelectorView;
import lombok.Getter;
import org.bson.conversions.Bson;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Updates.inc;

@Getter
public final class MatchingController implements Match.MatchBroadcaster, ModeSelectorView.ModeSelectedConsumer {
    private final Practice plugin;
    private final Random random = new Random();

    private final Map<MatchType, MatchSelector> selectors;
    private final Map<MatchType, MatchQueue<?>> queues;

    private final Map<UUID, Match> currentMatches;
    private final Map<Player, Match> spectating;
    private final Map<UUID, PlayerMatchStatsGUI.PlayerMatchStatsSnapshot> playerSnapshots;

    private final Multimap<Player, KitDocument> maySelectKits;

    public MatchingController(Practice plugin) throws ControllerInitException {
        this.plugin = plugin;

        this.selectors = new HashMap<>();
        this.queues = new HashMap<>();
        for (MatchType matchType : MatchType.values()) {
            final String prefix = "modes." + (matchType.isRanked() ? "ranked" : "unranked") + ".selector";
            selectors.put(matchType, new MatchSelector(plugin, listSupportedModes(matchType), matchType, this, prefix));

            if (matchType.isJoinable() && matchType.isRanked()) {
                queues.put(matchType, new RankedMatchQueue(matchType.isSingle() ? 2 : 4));
            } else if (matchType.isJoinable() && matchType.isUnranked()) {
                queues.put(matchType, new UnrankedMatchQueue(matchType.isSingle() ? 2 : 4));
            }
        }

        this.currentMatches = new HashMap<>();
        this.spectating = new HashMap<>();
        this.playerSnapshots = new HashMap<>();

        this.maySelectKits = ArrayListMultimap.create();
    }

    private List<MatchMode> listSupportedModes(MatchType matchType) {
        return plugin.getConfiguration().matchModes.stream()
                .filter(mode -> mode.supportTypes.contains(matchType))
                .collect(Collectors.toList());
    }

    // match creation

    public Match createMatch(MatchMode matchMode, MatchType matchType, String arenaName, List<Player> team1, List<Player> team2)
            throws MatchCreationException {
        if (arenaName == null && !matchMode.arenas.isEmpty()) {
            arenaName = matchMode.arenas.get(random.nextInt(matchMode.arenas.size()));
        }

        final ArenaController arenaController = plugin.getArenaController();
        final Arena arena = arenaName != null ? arenaController.getArenas().get(arenaName) : null;
        if (arena == null) {
            throw new MatchCreationException("match.create.failed.arena");
        }
        if (arena.getSpawnLocations().size() < 2) {
            throw new MatchCreationException("match.create.failed.missing-spawn");
        }

        final MatchArena matchArena = arenaController.makeMatchArena(arena);
        if (matchArena == null) {
            throw new MatchCreationException("match.create.failed.arena");
        }

        // make players forfeit their current matches
        final List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(team1);
        if (team2 != null) {
            allPlayers.addAll(team2);
        }
        allPlayers.forEach(this::resetState);
        allPlayers.forEach(itPlayer -> plugin.getVisibilityController().restrain(itPlayer, allPlayers));

        if (allPlayers.stream().distinct().count() != allPlayers.size()) {
            throw new MatchCreationException("match.create.failed.duplicate");
        }

        final MatchTeam matchTeam1 = new MatchTeam(team1.toArray(new Player[0]));
        final MatchTeam matchTeam2 = team2 != null ? new MatchTeam(team2.toArray(new Player[0])) : null;

        final World arenaWorld = arenaController.getWorld();
        Location centerPoint = matchArena.relativeLoc(arena.getCenterPoint(), arenaWorld);
        if (centerPoint != null) {
            final Block block = centerPoint.getBlock();
            if (block != null) {
                block.setType(arena.getConfiguration().centerBlockReplace);
            }
        } else {
            centerPoint = matchArena.relativeLoc(arena.getSpawnLocations().get(0), arenaWorld);
        }

        final Map<MatchTeam, Location> spawnLocations = new HashMap<>();
        spawnLocations.put(matchTeam1, matchArena.relativeLoc(arena.getSpawnLocations().get(0), arenaWorld));
        if (matchTeam2 != null) {
            spawnLocations.put(matchTeam2, matchArena.relativeLoc(arena.getSpawnLocations().get(1), arenaWorld));
        }
        spawnLocations.values().forEach(value -> value.add(0, 1, 0));
        arena.getSpawnLocations().stream()
                .map(rel -> matchArena.relativeLoc(rel, arenaWorld))
                .forEach(loc -> {
                    final Block block = loc.getBlock();
                    if (block != null) {
                        block.setType(arena.getConfiguration().spawnBlockReplace);
                    }
                });

        final Match match = new Match(matchMode, matchType, matchArena, centerPoint, spawnLocations,
                matchTeam1, matchTeam2, this);
        final ViewController viewController = plugin.getViewController();
        for (Player player : allPlayers) {
            viewController.getPlayerPanel(player).setPlayerView(null, null);
            player.closeInventory();
            plugin.getScoreboardController().destroyScoreboard(player);

            currentMatches.put(player.getUniqueId(), match);
            stuff(match, player);
            match.teleportToSpawn(player);
        }
        return match;
    }

    // queue

    public boolean isInQueue(Player player) {
        return queues.values().stream()
                .anyMatch(queue -> queue.isQueued(player));
    }

    public void removeFromQueue(Player player) {
        queues.values().stream()
                .filter(queue -> queue.isQueued(player))
                .findAny()
                .ifPresent(queue -> {
                    final MatchQueueEntry queueEntry = queue.getPlayerEntry(player);
                    final Collection<Player> removed;
                    if (queueEntry.getPlayers() instanceof PartyPlayerList) {
                        final PartyPlayerList partyPlayerList = (PartyPlayerList) queueEntry.getPlayers();
                        if (partyPlayerList.getParty().getLeader().equals(player) || partyPlayerList.size() <= 1) {
                            removed = queue.deQueuePlayer(player).getPlayers();
                        } else if (partyPlayerList.remove(player)) {
                            partyPlayerList.getParty().broadcast(plugin.getLocale().get("party.player-left-queue")
                                    .var("player", player.getName()));
                            removed = Collections.singletonList(player);
                        } else {
                            return;
                        }
                    } else {
                        removed = queue.deQueuePlayer(player).getPlayers();
                    }
                    removed.forEach(itPlayer -> plugin.getViewController().updateSpawnView(itPlayer, false));
                });
    }

    public boolean queue(Collection<Player> players, MatchType matchType, MatchMode matchMode, String arena, int elo) {
        final MatchQueue<?> matchQueue = queues.get(matchType);
        if (matchQueue == null) {
            return false;
        }

        if (matchQueue instanceof UnrankedMatchQueue) {
            ((UnrankedMatchQueue) matchQueue).push(new MatchQueueEntry(players, matchMode, arena));
        } else if (matchQueue instanceof RankedMatchQueue) {
            ((RankedMatchQueue) matchQueue).push(new RankedMatchQueueEntry(players, matchMode, arena, elo));
        } else {
            return false;
        }
        players.forEach(player -> plugin.getViewController().updateSpawnView(player, false));
        return true;
    }

    // queries

    public Match getCurrentMatch(Player player) {
        return currentMatches.get(player.getUniqueId());
    }

    public boolean isInMatch(Player player) {
        final Match currentMatch = getCurrentMatch(player);
        return currentMatch != null && currentMatch.getState() == MatchState.PLAYING;
    }

    public int getFightingPlayerCount() {
        return currentMatches.size();
    }

    public Collection<Match> getDistinctMatches() {
        return currentMatches.values().stream()
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean isSpawn(Player player) {
        return !isInMatch(player) && !spectating.containsKey(player);
    }


    // direct actions

    public void stuff(Match match, Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        if (match.getState() == MatchState.STARTING) {
            resetPlayer(player);
            maySelectKits.removeAll(player);
            plugin.getDatabaseController().async(db -> {
                final List<KitDocument> maySelect = new ArrayList<>();
                db.kits
                        .find(Filters.and(Filters.eq("owner", player.getUniqueId()), Filters.eq("modeId", match.getMode().id)))
                        .forEach((Consumer<? super KitDocument>) maySelect::add);
                maySelect.add(new KitDocument(null, null, 8, "Default", match.getMode().defaultKit));

                db.syncIfOnline(player, () -> {
                    final ItemPlaceholder kitIcon = new ItemPlaceholder(Material.BOOK, (short) 0, 1,
                            "${kit.choose.title}", "${kit.choose.desc}");
                    for (KitDocument kitDocument : maySelect) {
                        final Map<String, String> variables = new HashMap<>();
                        variables.put("name", kitDocument.getDisplayName());
                        final ItemStack kitItem = kitIcon.buildFromVariables(plugin.getLocale(), variables);
                        if (kitDocument.getSlot() == 8) {
                            final ItemMeta itemMeta = kitItem.getItemMeta();
                            itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
                            kitItem.setItemMeta(itemMeta);
                        }
                        player.getInventory().setItem(kitDocument.getSlot(), NMSUtil.setItemFlags(kitItem, NMSUtil.ItemFlag.HIDE_ENCHANTS));
                    }
                    maySelectKits.putAll(player, maySelect);
                });
            });
        } else if (match.getState() == MatchState.PLAYING) {
            final int noDamageTicks = match.getMode().noDamageTicks;
            if (noDamageTicks != -1) {
                player.setMaximumNoDamageTicks(noDamageTicks);
            }

            if (match.getMatchType() != MatchType.FFA) {
                match.teleportToSpawn(player);
            }
        }
    }

    public void selectKit(Player player, Kit kit) {
        player.getInventory().clear();
        kit.apply(player.getInventory(), true);
        player.updateInventory();
    }

    public void killPlayer(Player player, Match.DeathCause deathCause) {
        maySelectKits.removeAll(player);
        final Match match = currentMatches.get(player.getUniqueId());
        if (match == null) {
            return;
        }

        currentMatches.remove(player.getUniqueId());
        final MatchTeam matchTeam = match.getTeam2() != null ? match.getTeam(player) : match.getTeam1();
        if (matchTeam == null) {
            return;
        }

        if (matchTeam.getFightingPlayers().remove(player)) {
            matchTeam.getDeadPlayers().add(player);
            if (match.getState() == MatchState.ENDED) {
                return;
            }

            final Player killer = player.getKiller() != null && match.isAlive(player.getKiller())
                    ? player.getKiller() : null;
            final String messageKey;
            if (deathCause == Match.DeathCause.KILLED) {
                if (killer != null) {
                    messageKey = "match.game.death.killed";
                } else {
                    messageKey = "match.game.death.killed-no-killer";
                }
            } else if (deathCause == Match.DeathCause.FORFEIT) {
                messageKey = "match.game.death.forfeit";
            } else {
                throw new IllegalStateException();
            }

            match.getPlayerStatistics().get(player).dead = true;
            if (killer != null) {
                match.getPlayerStatistics().get(killer).kills++;
            }

            snapshot(match, player);
            spectate(player, match);
            match.getDead().add(player);
            match.broadcast(null, locale -> locale.get(messageKey)
                    .var("victim", player.getName())
                    .var("killer", killer != null ? killer.getName() : null)
                    .toString());

            match.getFightingPlayers().forEach(itPlayer -> plugin.getVisibilityController().hideIfShown(itPlayer, player));
            if (match.getTeam2() != null && match.getTeam1().getFightingPlayers().isEmpty()) {
                endMatch(match, match.getTeam2());
            } else if (match.getTeam2() != null && match.getTeam2().getFightingPlayers().isEmpty()) {
                endMatch(match, match.getTeam1());
            } else if (match.getTeam2() == null && match.getTeam1().getFightingPlayers().size() == 1) {
                endMatch(match, match.getTeam(match.getTeam1().getFightingPlayers().get(0)));
            }
        }
    }

    private void snapshot(Match match, Player player) {
        UUID temp;
        do {
            temp = UUID.randomUUID();
        } while (playerSnapshots.containsKey(temp));
        final UUID snapshotId = temp;

        final PlayerMatchStatsGUI.PlayerMatchStatsSnapshot snapshot = new PlayerMatchStatsGUI.PlayerMatchStatsSnapshot(
                snapshotId, player,
                player.getInventory().getContents(), player.getInventory().getArmorContents(),
                Math.max(0, player.getHealth()), match.getPlayerStatistics().get(player)
        );
        playerSnapshots.put(snapshotId, snapshot);
        match.getPlayerStatistics().values().stream()
                .filter(stats -> stats.snapshotId != null)
                .map(stats -> playerSnapshots.get(stats.snapshotId))
                .filter(itSnapshot -> itSnapshot.next == null)
                .findAny()
                .ifPresent(itSnapshot -> {
                    itSnapshot.next = snapshotId;
                    snapshot.previous = itSnapshot.id;
                });
        match.getPlayerStatistics().get(player).snapshotId = snapshotId;
    }

    public void endMatch(Match match, MatchTeam winningTeam) {
        match.setState(MatchState.ENDED);
        match.setPlaytime(0);

        for (Player player : match.getFightingPlayers()) {
            snapshot(match, player);
            resetPlayer(player);
        }

        // cycle through snapshots: tail <-> head
        PlayerMatchStatsGUI.PlayerMatchStatsSnapshot headSnapshot = null,
                tailSnapshot = null;
        for (PlayerMatchStats playerMatchStats : match.getPlayerStatistics().values()) {
            if (playerMatchStats.snapshotId != null) {
                PlayerMatchStatsGUI.PlayerMatchStatsSnapshot itSnapshot = playerSnapshots.get(playerMatchStats.snapshotId);
                if (itSnapshot.previous == null) {
                    headSnapshot = itSnapshot;
                }
                if (itSnapshot.next == null) {
                    tailSnapshot = itSnapshot;
                }
            }
        }
        if (headSnapshot != null && tailSnapshot != null) {
            tailSnapshot.next = headSnapshot.id;
            headSnapshot.previous = tailSnapshot.id;
        }

        // end message
        final LocaleController locale = plugin.getLocale();
        final LocaleController.MessageContext endMessageCtx = locale.get("match.end");
        for (int i = 0; i < 2; i++) {
            final Collection<OfflinePlayer> expansionPlayers = i == 0 ? winningTeam.getPlayers()
                    : match.getOpposingTeam(winningTeam).getPlayers();
            endMessageCtx
                    .expansion(i == 0 ? "winners" : "losers", expansionPlayers.stream()
                            .map(player -> (LocaleController.ExpansionElement) ctx -> ctx
                                    .var("player", player.getName())
                                    .var("stats-id", match.getPlayerStatistics().get(player).snapshotId.toString())
                            )
                            .collect(Collectors.toList()));
        }
        match.getReceivers(null).forEach(endMessageCtx::send);

        // ranked stats
        if (match.getMatchType().isRanked()) {
            plugin.getDatabaseController().async(db -> {
                final Map<MatchTeam, Integer> teamBaseElos = new HashMap<>();
                final Bson modeQuery = Filters.and(Filters.eq("mode", match.getMode().id),
                        Filters.eq("aux", match.getMatchType().getAux()));
                for (MatchTeam team : match.getTeams()) {
                    teamBaseElos.put(team, db.getAverageElo(getTeamQuery(team, modeQuery)));
                }

                for (Map.Entry<OfflinePlayer, PlayerMatchStats> entry : match.getPlayerStatistics().entrySet()) {
                    final OfflinePlayer player = entry.getKey();
                    final PlayerMatchStats stats = entry.getValue();
                    final boolean won = winningTeam.isInTeam(player.getUniqueId());
                    final MatchTeam opposingTeam = match.getOpposingTeam(player);

                    // stats

                    Bson update = null;
                    if (stats.kills > 0) {
                        update = Updates.inc("kills", stats.kills);
                    }
                    if (stats.dead) {
                        final Bson localUpdate = inc("deaths", 1);
                        update = update != null ? Updates.combine(update, localUpdate) : localUpdate;
                    }
                    if (update != null) {
                        db.players.updateOne(Filters.eq("playerId", player.getUniqueId()), update);
                    }

                    // elo

                    final Bson query = Filters.and(Filters.eq("playerId", player.getUniqueId()), modeQuery);
                    final EloDocument eloDoc = db.elo.find(query).first();
                    final int baseElo = eloDoc != null ? eloDoc.getElo() : 1000;

                    final int opponentsElo = teamBaseElos.get(opposingTeam);
                    final int diff = Math.abs(opponentsElo - baseElo);
                    final int eloGain = (int) Math.min(5, Math.max(22, diff * 0.17));
                    if (eloDoc != null) {
                        db.elo.updateOne(query, Updates.inc("elo", won ? eloGain : -eloGain));
                    } else {
                        db.elo.insertOne(new EloDocument(player.getUniqueId(), match.getMode().id,
                                match.getMatchType().getAux(), baseElo + (won ? eloGain : -eloGain)));
                    }

                    plugin.getDatabaseController().queryCacheUpdate(player, false);
                }
            });
        }
    }

    private Bson getTeamQuery(MatchTeam team, Bson modeQuery) {
        Bson query = null;
        for (OfflinePlayer opponent : team.getPlayers()) {
            final Bson localQuery = Filters.and(Filters.eq("playerId", opponent.getUniqueId()), modeQuery);
            query = query != null ? Filters.or(query, localQuery) : localQuery;
        }
        return query;
    }

    // hooks

    public void resetState(Player player) {
        killPlayer(player, Match.DeathCause.FORFEIT);
        removeFromQueue(player);
        plugin.getVisibilityController().liftRestrain(player);

        final Match matchSpectate = spectating.remove(player);
        if (matchSpectate != null) {
            matchSpectate.getSpectators().remove(player);
        }
    }

    public void onDisconnect(Player player) {
        resetState(player);
        plugin.getPartyController().onDisconnect(player);
    }

    // player management

    public void spectate(Player player, Match match) {
        resetState(player);
        match.getSpectators().add(player);
        spectating.put(player, match);
    }

    public void autoGo(Player player) {
        if (spectating.containsKey(player)) {
            plugin.getScoreboardController().destroyScoreboard(player);
            resetPlayer(player);
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);

            final Match match = spectating.get(player);
            player.teleport(match.getCenterLocation());
        } else {
            sendToSpawn(player);
        }
    }

    public void sendToSpawn(Player player) {
        final World spawnWorld = Bukkit.getWorlds().get(0);
        player.teleport(spawnWorld.getSpawnLocation());
        resetPlayer(player);
        player.setAllowFlight(player.hasPermission("oxymore.practice.spawn-fly"));

        plugin.getViewController().updateSpawnView(player, true);
        plugin.getScoreboardController().destroyScoreboard(player);
        plugin.getScoreboardController().toLobby(player);
    }

    private void resetPlayer(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.setMaximumNoDamageTicks(20);
        player.setMaxHealth(20);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(10);
        player.setAllowFlight(false);
        player.setFallDistance(0);
        player.setLevel(0);
        player.setExp(0);
    }

    // impl

    @Override
    public void broadcast(Collection<Player> receivers, Function<LocaleController, String> getMessage) {
        final String message = getMessage.apply(plugin.getLocale());
        if (message == null) {
            return;
        }
        receivers.forEach(receiver -> receiver.sendMessage(message));
    }

    // callbacks

    @Override
    public void onModeSelect(Player player, MatchType matchType, MatchMode matchMode, String arena) {
        final Party party = plugin.getPartyController().getParty(player);
        if (party != null) {
            if (!party.getLeader().equals(player)) {
                plugin.getLocale().get("party.start-no-leader")
                        .send(player);
                return;
            }
        }

        final MatchQueue<?> matchQueue = queues.get(matchType);
        if (matchQueue == null) {
            return;
        }

        final Collection<Player> players;
        if (party != null) {
            players = new PartyPlayerList(party);
            final int maxPlayers = matchQueue.getRequiredPlayers();

            players.add(player);
            final List<Player> partyPlayers = party.getPlayers();
            for (int i = 0; i < partyPlayers.size(); i++) {
                final Player itPlayer = partyPlayers.get(i);
                if (players.contains(itPlayer)) {
                    continue;
                }

                // priorities goes to fulfilling the queue rather than letting players finish their match
                // but try to avoid queuing players already in match if the number of players allows it
                final int remaining = (int) partyPlayers.stream()
                        .filter(itPartyPlayer -> !players.contains(itPartyPlayer))
                        .count();
                if (remaining > maxPlayers && getCurrentMatch(player) != null) {
                    continue;
                }
                players.add(itPlayer);
            }
        } else {
            players = Collections.singletonList(player);
        }

        if (players instanceof PartyPlayerList && players.size() > matchQueue.getRequiredPlayers() / 2) {
            plugin.getLocale().get("party.prevent-boost")
                    .send(player);
            return;
        }

        plugin.getDatabaseController().async(db -> {
            final int elo;
            if (matchType.isRanked()) {
                final Bson filter = Filters.and(
                        Filters.and(players.stream()
                                .map(itPlayer -> Filters.eq("playerId", itPlayer.getUniqueId()))
                                .collect(Collectors.toList())),
                        Filters.eq("mode", matchMode.id),
                        Filters.eq("aux", matchType.getAux()));
                elo = db.getAverageElo(filter);
            } else {
                elo = -1;
            }
            db.syncIfOnline(player, () -> {
                if (!Objects.equals(party, plugin.getPartyController().getParty(player)) ||
                        (party != null && !party.getLeader().equals(player))) {
                    return;
                }

                if (players instanceof PartyPlayerList) {
                    //noinspection ConstantConditions
                    players.removeIf(itPlayer -> !itPlayer.isOnline());
                }
                if (queue(players, matchType, matchMode, arena, elo)) {
                    plugin.getLocale().get("join.queued." + (matchType.isRanked() ? "ranked" : "unranked"))
                            .var("mode", matchMode.name)
                            .var("mode_aux", matchType.getAux())
                            .var("arena", arena)
                            .send(players);
                }
            });
        });
    }

    // classes

    public static class MatchCreationException extends Exception {
        public MatchCreationException(String messageKey) {
            super(messageKey);
        }
    }
}
