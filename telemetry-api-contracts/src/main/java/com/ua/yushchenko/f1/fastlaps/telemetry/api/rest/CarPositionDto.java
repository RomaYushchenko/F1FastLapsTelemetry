package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST/WebSocket DTO for one car's world position (B9 Live Track Map).
 * worldPosX/Z are in game world coordinates.
 * Optional racingNumber and driverLabel from Participants packet (packetId=4).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarPositionDto {

    private int carIndex;
    private float worldPosX;
    private float worldPosZ;
    /** Race number of the car (from Participants m_raceNumber). Optional. */
    private Integer racingNumber;
    /** Driver/participant name (from Participants m_name). Optional; used for map legend. */
    private String driverLabel;
}
