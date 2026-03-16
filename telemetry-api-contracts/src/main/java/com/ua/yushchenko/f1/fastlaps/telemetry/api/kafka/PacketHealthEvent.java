package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published by udp-ingest-service to telemetry.packetHealth topic.
 * Processing service consumes it and stores packet loss ratio per session for diagnostics API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacketHealthEvent {

    /** Session UID from the game (same as in packet headers). */
    private long sessionUid;
    /** Packet loss ratio in range [0.0, 1.0]. */
    private double packetLossRatio;
    /** Time when the ratio was computed (millis since epoch). */
    private long timestampMillis;
}
