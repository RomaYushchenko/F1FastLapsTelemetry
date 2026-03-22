package com.ua.yushchenko.f1.fastlaps.telemetry.processing.util;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;

import java.util.List;

/**
 * Pure helpers for leaderboard timing: cumulative race time from laps, best lap resolution, and gap strings.
 */
public final class LeaderboardTimingHelper {

    public static final String GAP_LEAD = "LEAD";
    public static final String GAP_EMPTY = "—";

    private LeaderboardTimingHelper() {
    }

    /**
     * Sum of valid lap times (ms) for the car; excludes invalid laps and in-progress zeros.
     */
    public static int totalRaceTimeMs(int carIndex, List<Lap> allLaps) {
        int sum = 0;
        for (Lap lap : allLaps) {
            if (lap.getCarIndex() == null || lap.getCarIndex().intValue() != carIndex) {
                continue;
            }
            if (Boolean.TRUE.equals(lap.getIsInvalid())) {
                continue;
            }
            if (lap.getLapTimeMs() != null && lap.getLapTimeMs() > 0) {
                sum += lap.getLapTimeMs();
            }
        }
        return sum;
    }

    /**
     * Minimum valid lap time (ms) for the car from lap rows.
     */
    public static Integer bestLapTimeMsFromLaps(int carIndex, List<Lap> allLaps) {
        Integer min = null;
        for (Lap lap : allLaps) {
            if (lap.getCarIndex() == null || lap.getCarIndex().intValue() != carIndex) {
                continue;
            }
            if (Boolean.TRUE.equals(lap.getIsInvalid())) {
                continue;
            }
            if (lap.getLapTimeMs() == null || lap.getLapTimeMs() <= 0) {
                continue;
            }
            if (min == null || lap.getLapTimeMs() < min) {
                min = lap.getLapTimeMs();
            }
        }
        return min;
    }

    /**
     * Prefer {@code sessionSummaryBestMs} when positive; otherwise lap-derived best.
     */
    public static Integer resolveBestLapTimeMs(Integer sessionSummaryBestMs, Integer lapDerivedBestMs) {
        if (sessionSummaryBestMs != null && sessionSummaryBestMs > 0) {
            return sessionSummaryBestMs;
        }
        if (lapDerivedBestMs != null && lapDerivedBestMs > 0) {
            return lapDerivedBestMs;
        }
        return null;
    }

    /**
     * Last-lap gap vs P1: P1 {@link #GAP_LEAD}; others delta vs leader's last lap time,
     * {@link #GAP_EMPTY} when lap times missing or car slower on last lap than leader (negative delta).
     */
    public static String formatLastLapGap(int position, Integer leaderLastLapMs, Integer carLastLapMs) {
        if (position == 1) {
            return GAP_LEAD;
        }
        if (leaderLastLapMs == null || leaderLastLapMs <= 0 || carLastLapMs == null || carLastLapMs <= 0) {
            return GAP_EMPTY;
        }
        int deltaMs = carLastLapMs - leaderLastLapMs;
        if (deltaMs < 0) {
            return GAP_EMPTY;
        }
        if (deltaMs == 0) {
            return "+" + formatDurationMs(0);
        }
        return "+" + formatDurationMs(deltaMs);
    }

    /**
     * Cumulative race gap vs P1: P1 {@link #GAP_LEAD}; others {@code +duration} behind leader total,
     * {@code +0.00} when equal, {@link #GAP_EMPTY} when data missing or car total less than leader (inconsistent).
     */
    public static String formatCumulativeRaceGap(int position, Integer leaderTotalMs, int carTotalMs) {
        if (position == 1) {
            return GAP_LEAD;
        }
        if (leaderTotalMs == null || leaderTotalMs <= 0 || carTotalMs <= 0) {
            return GAP_EMPTY;
        }
        int deltaMs = carTotalMs - leaderTotalMs;
        if (deltaMs < 0) {
            return GAP_EMPTY;
        }
        if (deltaMs == 0) {
            return "+" + formatDurationMs(0);
        }
        return "+" + formatDurationMs(deltaMs);
    }

    private static String formatDurationMs(int ms) {
        int sec = ms / 1000;
        int frac = (ms % 1000) / 10;
        if (sec >= 60) {
            int min = sec / 60;
            sec = sec % 60;
            return String.format("%d:%02d.%02d", min, sec, frac);
        }
        return String.format("%d.%02d", sec, frac);
    }
}
