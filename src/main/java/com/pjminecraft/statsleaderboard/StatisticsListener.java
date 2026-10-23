package com.pjminecraft.statsleaderboard;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

final class StatisticsListener implements Listener {
    private static final double MIN_TRACKED_DISTANCE = 0.0001D;
    private static final double MAX_TRACKED_DISTANCE_PER_MOVE = 16.0D;

    private final StatisticsStore store;

    StatisticsListener(StatisticsStore store) {
        this.store = store;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBroken(BlockBreakEvent event) {
        Player player = event.getPlayer();
        store.recordMinedBlock(player.getUniqueId(), player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }
        double distance = from.toVector().distance(to.toVector());
        if (distance < MIN_TRACKED_DISTANCE || distance > MAX_TRACKED_DISTANCE_PER_MOVE) {
            return;
        }
        Player player = event.getPlayer();
        store.recordDistance(player.getUniqueId(), player.getName(), distance);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        store.recordDeath(player.getUniqueId(), player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || killer.getUniqueId().equals(event.getEntity().getUniqueId())) {
            return;
        }
        store.recordKill(killer.getUniqueId(), killer.getName());
    }
}
