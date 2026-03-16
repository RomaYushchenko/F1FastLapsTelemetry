package com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics;

import java.util.Optional;

/**
 * Abstraction for reading packet loss metrics from observability backend.
 */
public interface PacketLossMetricsReader {

    /**
     * Returns packet loss ratio for the given session UID in range [0.0, 1.0].
     */
    Optional<Double> getPacketLossRatioBySessionUid(long sessionUid);
}

