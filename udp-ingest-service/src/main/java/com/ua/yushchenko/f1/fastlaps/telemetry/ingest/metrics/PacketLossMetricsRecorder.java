package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records packet loss statistics per session and exposes them as Micrometer gauges.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PacketLossMetricsRecorder {

    public static final String METRIC_NAME = "f1.telemetry.packet_loss_ratio";
    private static final Duration DEFAULT_WINDOW = Duration.ofSeconds(30);

    private final MeterRegistry meterRegistry;
    private final Clock clock;

    private final Map<Long, RollingWindowStats> statsBySession = new ConcurrentHashMap<>();

    /**
     * Records that a frame was expected for the given session.
     */
    public void recordExpectedFrame(long sessionUid) {
        updateStats(sessionUid, 1, 0);
    }

    /**
     * Records that a frame was successfully received for the given session.
     */
    public void recordReceivedFrame(long sessionUid) {
        updateStats(sessionUid, 0, 1);
    }

    /**
     * Records that one expected frame was not received for the given session.
     */
    public void recordLostFrame(long sessionUid) {
        updateStats(sessionUid, 1, 0);
    }

    private void updateStats(long sessionUid, long expectedDelta, long receivedDelta) {
        long nowMillis = clock.millis();
        RollingWindowStats stats = statsBySession.computeIfAbsent(sessionUid, uid -> {
            RollingWindowStats created = new RollingWindowStats(DEFAULT_WINDOW);
            registerGauge(uid, created);
            return created;
        });

        stats.addSample(nowMillis, expectedDelta, receivedDelta);
    }

    private void registerGauge(long sessionUid, RollingWindowStats stats) {
        try {
            Gauge.builder(METRIC_NAME, stats, RollingWindowStats::getPacketLossRatio)
                    .tag("session_uid", String.valueOf(sessionUid))
                    .register(meterRegistry);
        } catch (IllegalArgumentException e) {
            log.warn("Gauge for session_uid={} already registered, skipping", sessionUid, e);
        }
    }

    /**
     * Returns current packet loss ratio for the session in range [0.0, 1.0].
     * Used by packet-health publisher to send metrics to processing via Kafka.
     */
    public java.util.Optional<Double> getPacketLossRatio(long sessionUid) {
        RollingWindowStats stats = statsBySession.get(sessionUid);
        if (stats == null) {
            return java.util.Optional.empty();
        }
        double ratio = stats.getPacketLossRatio();
        if (Double.isNaN(ratio)) {
            return java.util.Optional.empty();
        }
        double clamped = Math.max(0.0d, Math.min(1.0d, ratio));
        return java.util.Optional.of(clamped);
    }

    /**
     * Returns session UIDs that currently have rolling-window stats (active sessions).
     */
    public java.util.Set<Long> getActiveSessionUids() {
        return new java.util.HashSet<>(statsBySession.keySet());
    }
}

