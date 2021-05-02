package com.oxymore.practice.listener;

import com.google.common.base.Preconditions;
import com.oxymore.practice.Practice;
import com.oxymore.practice.controller.MatchingController;
import com.oxymore.practice.documents.KitDocument;
import com.oxymore.practice.match.Match;
import com.oxymore.practice.match.MatchState;
import com.oxymore.practice.match.MatchTeam;
import com.oxymore.practice.match.PlayerMatchStats;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.NumberConversions;

public final class MatchListener implements Listener {
    private final Practice plugin;
    private final MatchingController matchingController;

    public MatchListener(Practice plugin) {
        this.plugin = plugin;
        this.matchingController = plugin.getMatchingController();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            final Player player = (Player) event.getEntity();
            if (!isInMatch(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        final Player player = (Player) event.getEntity();
        final Match playerMatch = matchingController.getCurrentMatch(player);
        if (playerMatch == null) {
            event.setCancelled(true);
            return;
        }

        if (event.getDamager() instanceof FishHook) {
            return;
        }

        Player damager = null;
        if (event.getDamager() instanceof Projectile) {
            final ProjectileSource projectileSource = ((Projectile) event.getDamager()).getShooter();
            if (projectileSource instanceof Player) {
                damager = (Player) projectileSource;
            }
        } else if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        }

        if (damager == null || !playerMatch.isAlive(damager)) {
            event.setCancelled(true);
            return;
        }

        final MatchTeam playerTeam = playerMatch.getTeam(player);
        if (playerTeam.equals(playerMatch.getTeam(damager))) {
            event.setCancelled(true);
        } else if (event.getDamager() instanceof Arrow) {
            plugin.getLocale().get("match.game.damage.bow")
                    .var("target", player.getName())
                    .var("health", String.valueOf(((int) player.getHealth() * 10) / 10.))
                    .var("hearts", String.valueOf(((int) (player.getHealth() / 2 * 10)) / 10.))
                    .send(damager);
        }

        if (!event.isCancelled()) {
            final PlayerMatchStats playerStats = playerMatch.getPlayerStatistics().get(player);
            playerStats.combo = 0;
            final PlayerMatchStats damagerStats = playerMatch.getPlayerStatistics().get(damager);
            damagerStats.hits++;
            damagerStats.combo++;
            if (damagerStats.combo > damagerStats.longestCombo) {
                damagerStats.longestCombo = damagerStats.combo;
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Match playerMatch = matchingController.getCurrentMatch(player);
        if (playerMatch == null) {
            return;
        }

        if (matchingController.getMaySelectKits().containsKey(player)) {
            if (event.getItem() != null && event.getItem().getType() == Material.BOOK &&
                    (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                final int slot = player.getInventory().getHeldItemSlot();
                final KitDocument selectedKit = matchingController.getMaySelectKits().get(player).stream()
                        .filter(kit -> kit.getSlot() == slot)
                        .findAny().orElse(null);
                if (selectedKit != null) {
                    plugin.getLocale().get("kit.selected")
                            .var("name", selectedKit.getDisplayName())
                            .send(player);
                    matchingController.selectKit(player, selectedKit.getKit());
                    matchingController.getMaySelectKits().removeAll(player);
                }
            }
        }

        if (event.getItem() == null || event.getItem().getType() != Material.ENDER_PEARL) {
            return;
        }

        if (playerMatch.getState() != MatchState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Material itemType = event.hasItem() ? event.getItem().getType() : null;
            if (itemType == Material.ENDER_PEARL) {
                final PlayerMatchStats playerStats = playerMatch.getPlayerStatistics().get(player);
                if (playerStats.enderpearlCooldown > 0) {
                    matchingController.getPlugin().getLocale().get("match.game.enderpearl-cooldown")
                            .var("seconds", String.valueOf(playerStats.enderpearlCooldown))
                            .send(player);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getEntity().getShooter();
        final Match playerMatch = matchingController.getCurrentMatch(player);
        if (playerMatch == null) {
            return;
        }

        final PlayerMatchStats playerStats = playerMatch.getPlayerStatistics().get(player);
        if (event.getEntity() instanceof EnderPearl) {
            playerStats.enderpearlCooldown = playerMatch.getMode().enderpearlCooldown;
            player.setLevel(playerStats.enderpearlCooldown);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        final Player player = (Player) event.getEntity().getShooter();
        final Match playerMatch = matchingController.getCurrentMatch(player);
        if (playerMatch == null) {
            event.setCancelled(true);
            return;
        }

        playerMatch.getPlayerStatistics().get(player).potions++;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDie(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        event.setKeepLevel(true); // prevent exp from dropping
        event.setKeepInventory(true);
        matchingController.killPlayer(event.getEntity(), Match.DeathCause.KILLED);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> event.getEntity().spigot().respawn(), 2);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> matchingController.autoGo(event.getPlayer()), 2);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        final Match playerMatch = matchingController.getCurrentMatch(player);
        if (playerMatch == null || playerMatch.getState() != MatchState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        if (event.getItemDrop().getItemStack().getType() == Material.GLASS_BOTTLE) {
            plugin.getServer().getScheduler().runTask(plugin, () -> event.getItemDrop().remove());
        } else {
            playerMatch.getEdited().getDroppedItem().add(event.getItemDrop());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (!isInMatch(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        allowForMutable(event, event.getPlayer());
    }

    @SuppressWarnings({"deprecation"})
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        allowForMutable(event, event.getPlayer());
        if (!event.isCancelled()) {
            final Block placedBlock = event.getBlock();
            if (placedBlock.getType() == Material.GLASS && placedBlock.getData() == 0) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketDispense(PlayerBucketEmptyEvent event) {
        allowForMutable(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        allowForMutable(event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHealthRegenerate(EntityRegainHealthEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        final Player player = (Player) event.getEntity();
        final Match playerMatch = matchingController.getCurrentMatch(player);
        if (playerMatch != null) {
            final boolean allowed = playerMatch.getState() == MatchState.PLAYING && playerMatch.getMode().regeneration;
            if (!allowed) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // if the player wants to jump, so be it
        // we only care if he moves
        if (getDistanceXZ(event.getFrom(), event.getTo()) >= 0.05) {
            final Player player = event.getPlayer();
            final Match playerMatch = matchingController.getCurrentMatch(player);
            if (playerMatch == null) {
                return;
            }

            if (playerMatch.getState() == MatchState.PLAYING) {
                if (event.getTo().getBlock().isLiquid() && playerMatch.getMode().fluidKills) {
                    player.damage(player.getHealth());
                }
            }
        }
    }

    private double getDistanceXZ(Location from, Location to) {
        return Math.sqrt(NumberConversions.square(from.getX() - to.getX()) +
                NumberConversions.square(from.getZ() - to.getZ()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!isInMatch((Player) event.getEntity())) {
            event.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExperienceGain(PlayerExpChangeEvent event) {
        if (!isInMatch(event.getPlayer())) {
            event.setAmount(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || event.getWhoClicked() == null) { // drop
            event.setCancelled(true);
            return;
        }

        final Player player = (Player) event.getWhoClicked();
        if (isInMatch(player) && event.getClickedInventory().equals(player.getInventory())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMonsterSpawn(EntitySpawnEvent event) {
        switch (event.getEntityType()) {
            case PLAYER:
            case DROPPED_ITEM:
            case EXPERIENCE_ORB:
                break;
            default:
                event.setCancelled(true);
        }
    }

    private boolean isInMatch(Player player) {
        final Match currentMatch = matchingController.getCurrentMatch(player);
        return currentMatch != null && currentMatch.getState() == MatchState.PLAYING && currentMatch.isAlive(player);
    }

    private void allowForMutable(Cancellable event, Player player) {
        Preconditions.checkNotNull(event);
        Preconditions.checkNotNull(player);
        final Match playerMatch = matchingController.getCurrentMatch(player);
        if (playerMatch != null) {
            event.setCancelled(!playerMatch.getMode().terrainMutable);
        } else {
            event.setCancelled(true);
        }
    }
}
