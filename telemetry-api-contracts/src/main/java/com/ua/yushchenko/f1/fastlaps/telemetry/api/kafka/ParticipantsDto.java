package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload for participants event: list of up to 22 participants (by car index).
 * F1 25 PacketParticipantsData.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantsDto {

    /** Number of active cars (m_numActiveCars). */
    private int numActiveCars;
    /** One entry per car index (0–21). */
    private List<ParticipantDataDto> participants;
}
