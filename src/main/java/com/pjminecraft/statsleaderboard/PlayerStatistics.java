package com.pjminecraft.statsleaderboard;

import java.util.UUID;

final class PlayerStatistics {
    private final UUID playerId;
    private String playerName;
    private long blocksMined;
    private double distanceMoved;
    private long deaths;

    PlayerStatistics(UUID playerId, String playerName) {
        this(playerId, playerName, 0L, 0.0D, 0L);
    }

    PlayerStatistics(UUID playerId, String playerName, long blocksMined, double distanceMoved, long deaths) {
        this.playerId = playerId;
        this.playerName = normalizeName(playerName, playerId);
        this.blocksMined = Math.max(0L, blocksMined);
        this.distanceMoved = Double.isFinite(distanceMoved) ? Math.max(0.0D, distanceMoved) : 0.0D;
        this.deaths = Math.max(0L, deaths);
    }

    UUID playerId() {
        return playerId;
    }

    String playerName() {
        return playerName;
    }

    long blocksMined() {
        return blocksMined;
    }

    double distanceMoved() {
        return distanceMoved;
    }

    long deaths() {
        return deaths;
    }

    boolean updatePlayerName(String playerName) {
        String normalized = normalizeName(playerName, playerId);
        if (this.playerName.equals(normalized)) {
            return false;
        }
        this.playerName = normalized;
        return true;
    }

    void addMinedBlock() {
        if (blocksMined < Long.MAX_VALUE) {
            blocksMined++;
        }
    }

    void addDistance(double distance) {
        if (!Double.isFinite(distance) || distance <= 0.0D) {
            return;
        }
        distanceMoved = Math.min(Double.MAX_VALUE, distanceMoved + distance);
    }

    void addDeath() {
        if (deaths < Long.MAX_VALUE) {
            deaths++;
        }
    }

    private static String normalizeName(String playerName, UUID playerId) {
        if (playerName == null || playerName.isBlank()) {
            return playerId.toString();
        }
        return playerName;
    }
}
