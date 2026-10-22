package com.pjminecraft.statsleaderboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class StatsLeaderboardPlugin extends JavaPlugin implements Listener {
    private StatisticsStore store;
    private LeaderboardHud hud;

    @Override
    public void onEnable() {
        store = new StatisticsStore(new File(getDataFolder(), "statistics.yml"), getLogger());
        store.load();
        hud = new LeaderboardHud(this, store);
        hud.start();
        LeaderboardCommand command = new LeaderboardCommand(store);
        getServer().getPluginManager().registerEvents(new StatisticsListener(store), this);
        getServer().getPluginManager().registerEvents(this, this);
        var pluginCommand = getCommand("statsleaderboard");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            hud.show(player);
        }
        Bukkit.getScheduler().runTaskTimer(this, store::saveIfDirty, 600L, 600L);
        getLogger().info("统计排行榜已启用");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.save();
        }
        if (hud != null) {
            hud.shutdown();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        store.getOrCreate(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        hud.show(event.getPlayer());
    }
}
