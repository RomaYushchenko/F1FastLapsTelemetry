package com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics;

import java.util.Optional;

/**
 * In-memory store for packet loss ratio per session, populated by PacketHealthConsumer.
 */
public interface PacketLossRatioStore {

    void set(long sessionUid, double packetLossRatio);

    Optional<Double> get(long sessionUid);
}
