package com.ua.yushchenko.f1.fastlaps.telemetry.processing.util;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.CAR_INDEX;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.CAR_INDEX_1;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_UID;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LeaderboardTimingHelper")
class LeaderboardTimingHelperTest {

    @Test
    @DisplayName("totalRaceTimeMs сумує лише валідні кола для carIndex")
    void totalRaceTimeMs_sumsValidLapsForCar() {
        Lap a = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 1)
                .lapTimeMs(60_000)
                .isInvalid(false)
                .build();
        Lap b = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 2)
                .lapTimeMs(30_000)
                .isInvalid(false)
                .build();
        Lap other = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 1)
                .lapTimeMs(99_000)
                .isInvalid(false)
                .build();

        int total = LeaderboardTimingHelper.totalRaceTimeMs(CAR_INDEX, List.of(a, b, other));

        assertThat(total).isEqualTo(90_000);
    }

    @Test
    @DisplayName("formatCumulativeRaceGap для P2 позаду лідера повертає +delta")
    void formatCumulativeRaceGap_returnsPlusWhenBehind() {
        String g = LeaderboardTimingHelper.formatCumulativeRaceGap(2, 90_000, 91_000);
        assertThat(g).isEqualTo("+1.00");
    }

    @Test
    @DisplayName("formatCumulativeRaceGap при від'ємній дельті повертає —")
    void formatCumulativeRaceGap_returnsDashWhenNegativeDelta() {
        String g = LeaderboardTimingHelper.formatCumulativeRaceGap(2, 90_000, 30_000);
        assertThat(g).isEqualTo("—");
    }

    @Test
    @DisplayName("resolveBestLapTimeMs віддає пріоритет session summary")
    void resolveBestLapTimeMs_prefersSummary() {
        Integer r = LeaderboardTimingHelper.resolveBestLapTimeMs(88_000, 90_000);
        assertThat(r).isEqualTo(88_000);
    }

    @Test
    @DisplayName("formatLastLapGap для P2 з дельтою останнього кола повертає +duration")
    void formatLastLapGap_returnsPlusWhenSlowerOnLastLap() {
        String g = LeaderboardTimingHelper.formatLastLapGap(2, 70_000, 71_000);
        assertThat(g).isEqualTo("+1.00");
    }

    @Test
    @DisplayName("formatLastLapGap при швидшому останньому колі за лідера повертає —")
    void formatLastLapGap_returnsDashWhenFasterLastLapThanLeader() {
        String g = LeaderboardTimingHelper.formatLastLapGap(2, 90_000, 89_000);
        assertThat(g).isEqualTo("—");
    }
}
