package com.pjminecraft.statsleaderboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

enum LeaderboardMetric {
    BLOCKS_MINED,
    DISTANCE_MOVED,
    KILLS,
    DEATHS
}

record LeaderboardEntry(int rank, String playerName, long blocksMined, double distanceMoved, long kills, long deaths) {
    double value(LeaderboardMetric metric) {
        return switch (metric) {
            case BLOCKS_MINED -> blocksMined;
            case DISTANCE_MOVED -> distanceMoved;
            case KILLS -> kills;
            case DEATHS -> deaths;
        };
    }
}

final class LeaderboardService {
    private LeaderboardService() {
    }

    static List<LeaderboardEntry> top(Collection<PlayerStatistics> statistics, LeaderboardMetric metric, int limit) {
        List<PlayerStatistics> sorted = new ArrayList<>(statistics);
        sorted.sort(comparatorFor(metric)
                .thenComparing(PlayerStatistics::playerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PlayerStatistics::playerId));
        int resultSize = Math.min(Math.max(0, limit), sorted.size());
        List<LeaderboardEntry> entries = new ArrayList<>(resultSize);
        for (int index = 0; index < resultSize; index++) {
            PlayerStatistics statisticsEntry = sorted.get(index);
            entries.add(new LeaderboardEntry(
                    index + 1,
                    statisticsEntry.playerName(),
                    statisticsEntry.blocksMined(),
                    statisticsEntry.distanceMoved(),
                    statisticsEntry.kills(),
                    statisticsEntry.deaths()
            ));
        }
        return List.copyOf(entries);
    }

    private static Comparator<PlayerStatistics> comparatorFor(LeaderboardMetric metric) {
        return switch (metric) {
            case BLOCKS_MINED -> Comparator.comparingLong(PlayerStatistics::blocksMined).reversed();
            case DISTANCE_MOVED -> Comparator.comparingDouble(PlayerStatistics::distanceMoved).reversed();
            case KILLS -> Comparator.comparingLong(PlayerStatistics::kills).reversed();
            case DEATHS -> Comparator.comparingLong(PlayerStatistics::deaths).reversed();
        };
    }
}
