package com.pjminecraft.statsleaderboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;

final class LeaderboardHud {
    private static final int ROWS = 3;
    private static final int NAME_PIXELS = 64;
    private static final int VALUE_PIXELS = CompactNumbers.width("888.8b");

    private final JavaPlugin plugin;
    private final StatisticsStore store;
    private Component header = Component.empty();

    LeaderboardHud(JavaPlugin plugin, StatisticsStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    void start() {
        clearSidebar();
        refresh();
        Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 20L, 20L);
    }

    void show(Player player) {
        player.sendPlayerListHeader(header);
    }

    void shutdown() {
        header = Component.empty();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeader(Component.empty());
        }
        clearSidebar();
    }

    private void refresh() {
        Component next = render();
        if (next.equals(header)) {
            return;
        }
        header = next;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeader(header);
        }
    }

    private Component render() {
        List<Component> lines = new ArrayList<>();
        section(lines, "挖掘", LeaderboardMetric.BLOCKS_MINED);
        section(lines, "移动", LeaderboardMetric.DISTANCE_MOVED);
        section(lines, "击杀", LeaderboardMetric.KILLS);
        section(lines, "死亡", LeaderboardMetric.DEATHS);
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private void section(List<Component> lines, String title, LeaderboardMetric metric) {
        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }
        lines.add(Component.text(title, NamedTextColor.GOLD));
        List<LeaderboardEntry> entries = LeaderboardService.top(store.all(), metric, ROWS);
        if (entries.isEmpty()) {
            lines.add(Component.text("暂无", NamedTextColor.GRAY));
            return;
        }
        for (LeaderboardEntry entry : entries) {
            String name = CompactNumbers.padRight(CompactNumbers.fit(entry.playerName(), NAME_PIXELS), NAME_PIXELS);
            String value = CompactNumbers.padLeft(CompactNumbers.format(entry.value(metric)), VALUE_PIXELS);
            lines.add(Component.text(entry.rank() + " " + name + " " + value, NamedTextColor.WHITE));
        }
    }

    private static void clearSidebar() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard current = player.getScoreboard();
            Objective sidebar = current.getObjective(DisplaySlot.SIDEBAR);
            if (sidebar != null && "slb".equals(sidebar.getName())) {
                player.setScoreboard(main);
            }
        }
    }
}
