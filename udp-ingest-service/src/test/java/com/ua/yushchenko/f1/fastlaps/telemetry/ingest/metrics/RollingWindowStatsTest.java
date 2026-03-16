package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RollingWindowStats")
class RollingWindowStatsTest {

    @Test
    @DisplayName("should return zero loss when no expected frames")
    void shouldReturnZeroLossWhenNoExpectedFrames() {
        RollingWindowStats stats = new RollingWindowStats(Duration.ofSeconds(30));

        stats.addSample(0L, 0, 0);

        assertThat(stats.getPacketLossRatio()).isEqualTo(0.0d);
        assertThat(stats.getTotalExpected()).isZero();
        assertThat(stats.getTotalReceived()).isZero();
    }

    @Test
    @DisplayName("should calculate partial loss correctly")
    void shouldCalculatePartialLossCorrectly() {
        RollingWindowStats stats = new RollingWindowStats(Duration.ofSeconds(30));

        stats.addSample(0L, 10, 8);

        assertThat(stats.getTotalExpected()).isEqualTo(10);
        assertThat(stats.getTotalReceived()).isEqualTo(8);
        assertThat(stats.getPacketLossRatio()).isEqualTo(0.2d);
    }

    @Test
    @DisplayName("should clamp loss ratio between 0 and 1")
    void shouldClampLossRatioBetweenZeroAndOne() {
        RollingWindowStats stats = new RollingWindowStats(Duration.ofSeconds(30));

        stats.addSample(0L, 10, 15);

        assertThat(stats.getPacketLossRatio()).isEqualTo(0.0d);
    }

    @Test
    @DisplayName("should evict samples outside window")
    void shouldEvictSamplesOutsideWindow() {
        RollingWindowStats stats = new RollingWindowStats(Duration.ofSeconds(30));

        stats.addSample(0L, 10, 5);
        stats.addSample(31_000L, 10, 10);

        assertThat(stats.getTotalExpected()).isEqualTo(10);
        assertThat(stats.getTotalReceived()).isEqualTo(10);
        assertThat(stats.getPacketLossRatio()).isEqualTo(0.0d);
    }

    @Test
    @DisplayName("should reject negative deltas")
    void shouldRejectNegativeDeltas() {
        RollingWindowStats stats = new RollingWindowStats(Duration.ofSeconds(30));

        assertThatThrownBy(() -> stats.addSample(0L, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stats.addSample(0L, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

