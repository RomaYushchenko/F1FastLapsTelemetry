package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One participant (car) from F1 25 PacketParticipantsData.
 * Used for live track map (race number on dot, driver name in legend).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantDataDto {

    /** Car index (0–21). */
    private int carIndex;
    /** Race number of the car (m_raceNumber). */
    private Integer raceNumber;
    /** Driver/participant name in UTF-8 (m_name), null if empty. */
    private String name;
    /** Driver id – see appendix; 255 if network human. */
    private Integer driverId;
    /** Team id – see appendix. */
    private Integer teamId;
}
