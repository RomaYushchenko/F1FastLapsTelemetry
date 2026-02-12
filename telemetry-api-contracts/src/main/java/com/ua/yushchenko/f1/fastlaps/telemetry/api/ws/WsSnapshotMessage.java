package com.ua.yushchenko.f1.fastlaps.telemetry.api.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * WebSocket server → client: live snapshot (10 Hz).
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 4.5.1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsSnapshotMessage {

    public static final String TYPE = "SNAPSHOT";

    private String type;
    private Instant timestamp;
    private Integer speedKph;
    private Integer gear;
    private Integer engineRpm;
    private Float throttle;
    private Float brake;
    /** DRS active (from car status); null if not yet received */
    private Boolean drs;
    private Integer currentLap;
    private Integer currentSector;
}
