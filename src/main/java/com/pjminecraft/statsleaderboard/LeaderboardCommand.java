package com.pjminecraft.statsleaderboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

final class LeaderboardCommand implements CommandExecutor, TabCompleter {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final StatisticsStore store;

    LeaderboardCommand(StatisticsStore store) {
        this.store = store;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("me")) {
            showOwn(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("top") && args.length >= 2) {
            LeaderboardMetric metric = metric(args[1]);
            if (metric == null) {
                sender.sendMessage(Component.text("用法：/" + label + " top <blocks|distance|kills|deaths> [数量]", NamedTextColor.RED));
                return true;
            }
            int limit = DEFAULT_LIMIT;
            if (args.length >= 3) {
                try {
                    limit = Integer.parseInt(args[2]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage(Component.text("数量要在 1 到 " + MAX_LIMIT + " 之间。", NamedTextColor.RED));
                    return true;
                }
            }
            if (limit < 1 || limit > MAX_LIMIT) {
                sender.sendMessage(Component.text("数量要在 1 到 " + MAX_LIMIT + " 之间。", NamedTextColor.RED));
                return true;
            }
            showTop(sender, metric, limit);
            return true;
        }
        sender.sendMessage(Component.text("用法：/" + label + " [me|top <blocks|distance|kills|deaths> [数量]]", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("me", "top"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return filter(List.of("blocks", "distance", "kills", "deaths"), args[1]);
        }
        return List.of();
    }

    private void showOwn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("这个命令只能由玩家使用。", NamedTextColor.RED));
            return;
        }
        if (ExcludedPlayers.isExcluded(player.getName())) {
            player.sendMessage(Component.text("不记录你的统计。", NamedTextColor.GRAY));
            return;
        }
        PlayerStatistics statistics = store.getOrCreate(player.getUniqueId(), player.getName());
        sender.sendMessage(Component.text(
                statistics.playerName()
                        + "：已挖掘 " + CompactNumbers.format(statistics.blocksMined())
                        + " 个方块，已移动 " + CompactNumbers.format(statistics.distanceMoved())
                        + " 米，击杀 " + CompactNumbers.format(statistics.kills())
                        + " 次，死亡 " + CompactNumbers.format(statistics.deaths()) + " 次"
        ));
    }

    private void showTop(CommandSender sender, LeaderboardMetric metric, int limit) {
        List<LeaderboardEntry> entries = LeaderboardService.top(store.all(), metric, limit);
        String header = switch (metric) {
            case BLOCKS_MINED -> "方块挖掘排行榜";
            case DISTANCE_MOVED -> "移动距离排行榜";
            case KILLS -> "击杀排行榜";
            case DEATHS -> "死亡排行榜";
        };
        sender.sendMessage(Component.text(header, NamedTextColor.GOLD));
        if (entries.isEmpty()) {
            sender.sendMessage(Component.text("目前还没有玩家统计数据"));
            return;
        }
        int namePixels = CompactNumbers.width("WWWWWWWWWWWW");
        int valuePixels = CompactNumbers.width("888.8b");
        for (LeaderboardEntry entry : entries) {
            String line = "#" + entry.rank() + " "
                    + CompactNumbers.padRight(CompactNumbers.fit(entry.playerName(), namePixels), namePixels)
                    + CompactNumbers.padLeft(CompactNumbers.format(entry.value(metric)), valuePixels);
            sender.sendMessage(Component.text(line));
        }
    }

    private static LeaderboardMetric metric(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "blocks", "block" -> LeaderboardMetric.BLOCKS_MINED;
            case "distance", "move" -> LeaderboardMetric.DISTANCE_MOVED;
            case "kills", "kill" -> LeaderboardMetric.KILLS;
            case "deaths", "death" -> LeaderboardMetric.DEATHS;
            default -> null;
        };
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(lower)).toList();
    }
}
