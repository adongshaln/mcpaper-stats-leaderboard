package com.pjminecraft.statsleaderboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class LeaderboardHud {
    private static final int ROWS = 5;
    private static final String TAG = "statsleaderboard";
    private static final double FORWARD = 1.6D;
    private static final double LEFT = 0.62D;
    private static final double UP = 0.42D;
    private static final int NAME_PIXELS = 72;
    private static final int VALUE_PIXELS = CompactNumbers.width("888.8b");
    private static final int RANK_PIXELS = CompactNumbers.width("10");

    private final JavaPlugin plugin;
    private final StatisticsStore store;
    private final Map<UUID, Panel> panels = new HashMap<>();

    LeaderboardHud(JavaPlugin plugin, StatisticsStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::followView, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshText, 20L, 20L);
    }

    void show(Player player) {
        Panel panel = panels.computeIfAbsent(player.getUniqueId(), ignored -> new Panel());
        ensureDisplay(player, panel);
        hideFromOthers(player, panel.display);
        refreshOne(player, panel);
    }

    void hideOthersFrom(Player player) {
        for (Panel panel : panels.values()) {
            if (panel.display != null && panel.display.isValid()) {
                player.hideEntity(plugin, panel.display);
            }
        }
    }

    void forget(Player player) {
        Panel panel = panels.remove(player.getUniqueId());
        if (panel != null && panel.display != null && panel.display.isValid()) {
            panel.display.remove();
        }
    }

    void shutdown() {
        for (Panel panel : panels.values()) {
            if (panel.display != null && panel.display.isValid()) {
                panel.display.remove();
            }
        }
        panels.clear();
    }

    private void followView() {
        Iterator<Map.Entry<UUID, Panel>> iterator = panels.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Panel> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                if (entry.getValue().display != null && entry.getValue().display.isValid()) {
                    entry.getValue().display.remove();
                }
                iterator.remove();
                continue;
            }
            TextDisplay display = ensureDisplay(player, entry.getValue());
            display.teleport(corner(player));
        }
    }

    private void refreshText() {
        for (Map.Entry<UUID, Panel> entry : panels.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                refreshOne(player, entry.getValue());
            }
        }
    }

    private void refreshOne(Player player, Panel panel) {
        TextDisplay display = ensureDisplay(player, panel);
        String text = render();
        if (text.equals(panel.text)) {
            return;
        }
        panel.text = text;
        display.text(Component.text(text, NamedTextColor.WHITE));
    }

    private TextDisplay ensureDisplay(Player player, Panel panel) {
        if (panel.display != null && panel.display.isValid()) {
            return panel.display;
        }
        TextDisplay display = player.getWorld().spawn(corner(player), TextDisplay.class, entity -> {
            entity.text(Component.text(""));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.LEFT);
            entity.setLineWidth(240);
            entity.setSeeThrough(true);
            entity.setShadowed(true);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(Color.fromARGB(90, 8, 12, 16));
            entity.setShadowRadius(0.0F);
            entity.setShadowStrength(0.0F);
            entity.setViewRange(0.4F);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setInterpolationDuration(0);
            entity.setTeleportDuration(0);
            entity.setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F),
                    new Vector3f(0.22F, 0.22F, 0.22F),
                    new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)
            ));
            entity.setPersistent(false);
            entity.addScoreboardTag(TAG);
        });
        panel.display = display;
        panel.text = "";
        hideFromOthers(player, display);
        return display;
    }

    private void hideFromOthers(Player owner, TextDisplay display) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(owner.getUniqueId())) {
                other.hideEntity(plugin, display);
            }
        }
    }

    private static Location corner(Player player) {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().clone().normalize();
        double yaw = Math.toRadians(eye.getYaw());
        Vector left = new Vector(Math.cos(yaw), 0.0D, Math.sin(yaw));
        if (left.lengthSquared() < 1.0E-8D) {
            left = new Vector(1.0D, 0.0D, 0.0D);
        }
        left.normalize();
        Vector up = forward.clone().crossProduct(left);
        if (up.lengthSquared() < 1.0E-8D) {
            up = new Vector(0.0D, 1.0D, 0.0D);
        } else {
            up.normalize();
        }
        Vector position = eye.toVector()
                .add(forward.multiply(FORWARD))
                .add(left.multiply(LEFT))
                .add(up.multiply(UP));
        return new Location(eye.getWorld(), position.getX(), position.getY(), position.getZ());
    }

    private String render() {
        StringBuilder text = new StringBuilder();
        section(text, "挖掘", LeaderboardMetric.BLOCKS_MINED);
        section(text, "移动", LeaderboardMetric.DISTANCE_MOVED);
        section(text, "死亡", LeaderboardMetric.DEATHS);
        return text.toString().stripTrailing();
    }

    private void section(StringBuilder text, String title, LeaderboardMetric metric) {
        if (!text.isEmpty()) {
            text.append('\n');
        }
        text.append(title);
        List<LeaderboardEntry> entries = LeaderboardService.top(store.all(), metric, ROWS);
        if (entries.isEmpty()) {
            text.append('\n').append(CompactNumbers.padRight("暂无", NAME_PIXELS));
            return;
        }
        for (LeaderboardEntry entry : entries) {
            String rank = CompactNumbers.padLeft(Integer.toString(entry.rank()), RANK_PIXELS);
            String name = CompactNumbers.padRight(CompactNumbers.fit(entry.playerName(), NAME_PIXELS), NAME_PIXELS);
            String value = CompactNumbers.padLeft(CompactNumbers.format(entry.value(metric)), VALUE_PIXELS);
            text.append('\n').append(rank).append(' ').append(name).append(value);
        }
    }

    private static final class Panel {
        private TextDisplay display;
        private String text = "";
    }
}
