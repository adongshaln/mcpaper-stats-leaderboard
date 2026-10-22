package com.pjminecraft.statsleaderboard;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

final class StatisticsStore {
    private final File file;
    private final Logger logger;
    private final Map<UUID, PlayerStatistics> players = new HashMap<>();
    private boolean dirty;

    StatisticsStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    void load() {
        players.clear();
        if (!file.isFile()) {
            return;
        }
        ConfigurationSection section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                ConfigurationSection player = section.getConfigurationSection(key);
                if (player == null) {
                    continue;
                }
                players.put(playerId, new PlayerStatistics(
                        playerId,
                        player.getString("name"),
                        player.getLong("blocks"),
                        player.getDouble("distance"),
                        player.getLong("deaths")
                ));
            } catch (IllegalArgumentException ignored) {
                logger.warning("跳过无效的统计玩家：" + key);
            }
        }
    }

    Collection<PlayerStatistics> all() {
        return java.util.List.copyOf(players.values());
    }

    PlayerStatistics getOrCreate(UUID playerId, String playerName) {
        PlayerStatistics statistics = players.get(playerId);
        if (statistics == null) {
            statistics = new PlayerStatistics(playerId, playerName);
            players.put(playerId, statistics);
            dirty = true;
        } else if (statistics.updatePlayerName(playerName)) {
            dirty = true;
        }
        return statistics;
    }

    void recordMinedBlock(UUID playerId, String playerName) {
        getOrCreate(playerId, playerName).addMinedBlock();
        dirty = true;
    }

    void recordDistance(UUID playerId, String playerName, double distance) {
        if (!Double.isFinite(distance) || distance <= 0.0D) {
            return;
        }
        getOrCreate(playerId, playerName).addDistance(distance);
        dirty = true;
    }

    void recordDeath(UUID playerId, String playerName) {
        getOrCreate(playerId, playerName).addDeath();
        dirty = true;
    }

    void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlayerStatistics statistics : players.values()) {
            String path = "players." + statistics.playerId();
            yaml.set(path + ".name", statistics.playerName());
            yaml.set(path + ".blocks", statistics.blocksMined());
            yaml.set(path + ".distance", statistics.distanceMoved());
            yaml.set(path + ".deaths", statistics.deaths());
        }
        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
            dirty = false;
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "无法保存排行榜统计", exception);
        }
    }
}
