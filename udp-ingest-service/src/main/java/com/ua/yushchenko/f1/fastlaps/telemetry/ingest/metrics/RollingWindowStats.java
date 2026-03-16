package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.metrics;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Time-based sliding window of expected and received frames.
 */
class RollingWindowStats {

    private final long windowMillis;
    private final Deque<Sample> samples = new ArrayDeque<>();

    private long totalExpected;
    private long totalReceived;

    RollingWindowStats(Duration window) {
        this.windowMillis = window.toMillis();
    }

    void addSample(long timestampMillis, long expectedDelta, long receivedDelta) {
        if (expectedDelta < 0 || receivedDelta < 0) {
            throw new IllegalArgumentException("Deltas must be non-negative");
        }

        if (expectedDelta == 0 && receivedDelta == 0) {
            return;
        }

        Sample sample = new Sample(timestampMillis, expectedDelta, receivedDelta);
        samples.addLast(sample);

        totalExpected += expectedDelta;
        totalReceived += receivedDelta;

        evictOldSamples(timestampMillis);
    }

    double getPacketLossRatio() {
        if (totalExpected <= 0) {
            return 0.0d;
        }

        double lossRatio = 1.0d - (double) totalReceived / Math.max(totalExpected, 1L);
        if (lossRatio < 0.0d) {
            return 0.0d;
        }
        if (lossRatio > 1.0d) {
            return 1.0d;
        }
        return lossRatio;
    }

    long getTotalExpected() {
        return totalExpected;
    }

    long getTotalReceived() {
        return totalReceived;
    }

    private void evictOldSamples(long nowMillis) {
        long threshold = nowMillis - windowMillis;
        while (!samples.isEmpty() && samples.peekFirst().timestampMillis < threshold) {
            Sample old = samples.removeFirst();
            totalExpected -= old.expectedDelta;
            totalReceived -= old.receivedDelta;
        }
    }

    private static final class Sample {
        private final long timestampMillis;
        private final long expectedDelta;
        private final long receivedDelta;

        private Sample(long timestampMillis, long expectedDelta, long receivedDelta) {
            this.timestampMillis = timestampMillis;
            this.expectedDelta = expectedDelta;
            this.receivedDelta = receivedDelta;
        }
    }
}

