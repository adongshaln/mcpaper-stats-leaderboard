package com.pjminecraft.statsleaderboard;

final class ExcludedPlayers {
    private ExcludedPlayers() {
    }

    static boolean isExcluded(String playerName) {
        return playerName != null && playerName.equalsIgnoreCase("adongtest");
    }
}
