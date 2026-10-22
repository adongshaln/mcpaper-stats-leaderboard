package com.pjminecraft.statsleaderboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

final class LeaderboardHud {
    private static final int ROWS = 3;
    private static final int NAME_PIXELS = 64;
    private static final int VALUE_PIXELS = CompactNumbers.width("888.8b");
    private static final int RANK_PIXELS = CompactNumbers.width("10");

    private final JavaPlugin plugin;
    private final StatisticsStore store;
    private final Scoreboard board;
    private final Objective objective;
    private final List<String> entries = new ArrayList<>();
    private String shown = "";

    LeaderboardHud(JavaPlugin plugin, StatisticsStore store) {
        this.plugin = plugin;
        this.store = store;
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = board.registerNewObjective("slb", Criteria.DUMMY, Component.text("排行榜", NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
    }

    void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 20L, 20L);
    }

    void show(Player player) {
        player.setScoreboard(board);
        refresh();
    }

    void shutdown() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboard() == board) {
                player.setScoreboard(main);
            }
        }
    }

    private void refresh() {
        List<Line> lines = render();
        String signature = signature(lines);
        if (signature.equals(shown) && lines.size() == entries.size()) {
            return;
        }
        shown = signature;
        while (entries.size() < lines.size()) {
            int index = entries.size();
            String entry = entryName(index);
            String teamName = "slb" + index;
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }
            entries.add(entry);
            objective.getScore(entry).setScore(lines.size() - index);
        }
        while (entries.size() > lines.size()) {
            String entry = entries.remove(entries.size() - 1);
            board.resetScores(entry);
        }
        for (int index = 0; index < lines.size(); index++) {
            Team team = board.getTeam("slb" + index);
            Line line = lines.get(index);
            if (team != null) {
                team.prefix(line.prefix());
                team.suffix(line.suffix());
            }
            objective.getScore(entries.get(index)).setScore(lines.size() - index);
        }
    }

    private List<Line> render() {
        List<Line> lines = new ArrayList<>();
        section(lines, "挖掘", LeaderboardMetric.BLOCKS_MINED);
        section(lines, "移动", LeaderboardMetric.DISTANCE_MOVED);
        section(lines, "死亡", LeaderboardMetric.DEATHS);
        return lines;
    }

    private void section(List<Line> lines, String title, LeaderboardMetric metric) {
        lines.add(new Line(Component.text(title, NamedTextColor.GOLD), Component.empty()));
        List<LeaderboardEntry> entries = LeaderboardService.top(store.all(), metric, ROWS);
        if (entries.isEmpty()) {
            lines.add(new Line(Component.text("暂无", NamedTextColor.GRAY), Component.empty()));
            return;
        }
        for (LeaderboardEntry entry : entries) {
            String rank = CompactNumbers.padLeft(Integer.toString(entry.rank()), RANK_PIXELS);
            String name = CompactNumbers.padRight(CompactNumbers.fit(entry.playerName(), NAME_PIXELS), NAME_PIXELS);
            String value = CompactNumbers.padLeft(CompactNumbers.format(entry.value(metric)), VALUE_PIXELS);
            lines.add(new Line(
                    Component.text(rank + " " + name, NamedTextColor.WHITE),
                    Component.text(value, NamedTextColor.YELLOW)
            ));
        }
    }

    private static String signature(List<Line> lines) {
        StringBuilder builder = new StringBuilder();
        for (Line line : lines) {
            builder.append(line.prefix()).append('|').append(line.suffix()).append('\n');
        }
        return builder.toString();
    }

    private static String entryName(int index) {
        return "§" + Integer.toHexString(index) + "§r";
    }

    private record Line(Component prefix, Component suffix) {
    }
}
