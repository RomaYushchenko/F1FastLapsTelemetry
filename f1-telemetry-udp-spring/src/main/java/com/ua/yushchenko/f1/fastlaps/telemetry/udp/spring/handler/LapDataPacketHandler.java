package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1PacketHandler;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1UdpListener;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;

/**
 * Handles Lap Data packets (packetId=2).
 * Parses lap times, sectors, and validity and publishes to telemetry.lap topic.
 * Packet layout per docs: .github/docs/F1 25 Telemetry Output Structures.txt — LapData (57 bytes per car).
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.lap.enabled", havingValue = "true", matchIfMissing = true)
public class LapDataPacketHandler {

    private static final String TOPIC = "telemetry.lap";
    /** LapData size in bytes. From docs: (1285 - 29 header - 2 timeTrial bytes) / 22 cars = 57. */
    private static final int LAP_DATA_SIZE = 57;

    private final TelemetryPublisher publisher;

    @F1PacketHandler(packetId = 2)
    public void handleLapDataPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing lap data packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        try {
            int playerCarIndex = header.getPlayerCarIndex();

            // Payload starts after 29-byte header; skip to player car data
            int offset = playerCarIndex * LAP_DATA_SIZE;
            if (payload.position() + offset + LAP_DATA_SIZE > payload.limit()) {
                log.warn("Lap data payload too short for car index {}: need {} bytes", playerCarIndex, offset + LAP_DATA_SIZE);
                return;
            }
            payload.position(payload.position() + offset);

            LapDto lapData = parseLapData(payload);
            
            LapDataEvent event = buildEvent(header, lapData);
            String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();
            
            publisher.publish(TOPIC, key, event);
            
            log.debug("Published lap data: lap={}, time={}ms, invalid={}", 
                    lapData.getLapNumber(), lapData.getCurrentLapTimeMs(), lapData.isInvalid());
        } catch (Exception e) {
            log.error("Failed to parse lap data packet: sessionUID={}, frame={}", 
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }
    
    private LapDto parseLapData(ByteBuffer buffer) {
        // F1 UDP is little-endian; ensure we read as LE (other handlers or slice may have changed order)
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        // F1 25 LapData — exact layout from .github/docs/F1 25 Telemetry Output Structures.txt
        buffer.getInt();                                  // uint32 m_lastLapTimeInMS
        int currentLapTimeMs = buffer.getInt();          // uint32 m_currentLapTimeInMS
        buffer.getShort();                               // uint16 m_sector1TimeMSPart
        buffer.get();                                    // uint8 m_sector1TimeMinutesPart
        buffer.getShort();                               // uint16 m_sector2TimeMSPart
        buffer.get();                                    // uint8 m_sector2TimeMinutesPart
        buffer.getShort();                               // uint16 m_deltaToCarInFrontMSPart
        buffer.get();                                    // uint8 m_deltaToCarInFrontMinutesPart
        buffer.getShort();                               // uint16 m_deltaToRaceLeaderMSPart
        buffer.get();                                    // uint8 m_deltaToRaceLeaderMinutesPart
        float lapDistance = buffer.getFloat();           // float m_lapDistance
        buffer.getFloat();                               // float m_totalDistance
        buffer.getFloat();                               // float m_safetyCarDelta
        buffer.get();                                    // uint8 m_carPosition
        int currentLapNum = buffer.get() & 0xFF;         // uint8 m_currentLapNum
        buffer.get();                                    // uint8 m_pitStatus
        buffer.get();                                    // uint8 m_numPitStops
        int sector = buffer.get() & 0xFF;                 // uint8 m_sector (0=S1, 1=S2, 2=S3)
        int currentLapInvalid = buffer.get() & 0xFF;    // uint8 m_currentLapInvalid (0=valid, 1=invalid)
        int penalties = buffer.get() & 0xFF;             // uint8 m_penalties
        buffer.get();                                    // uint8 m_totalWarnings
        buffer.get();                                    // uint8 m_cornerCuttingWarnings
        buffer.get();                                    // uint8 m_numUnservedDriveThroughPens
        buffer.get();                                    // uint8 m_numUnservedStopGoPens
        buffer.get();                                    // uint8 m_gridPosition
        buffer.get();                                    // uint8 m_driverStatus
        buffer.get();                                    // uint8 m_resultStatus
        buffer.get();                                    // uint8 m_pitLaneTimerActive
        buffer.getShort();                               // uint16 m_pitLaneTimeInLaneInMS
        buffer.getShort();                               // uint16 m_pitStopTimerInMS
        buffer.get();                                    // uint8 m_pitStopShouldServePen
        buffer.getFloat();                               // float m_speedTrapFastestSpeed
        buffer.get();                                    // uint8 m_speedTrapFastestLap

        return LapDto.builder()
                .lapNumber(currentLapNum)
                .lapDistance(lapDistance)
                .currentLapTimeMs(currentLapTimeMs > 0 ? currentLapTimeMs : null)
                .sector(sector)
                .isInvalid(currentLapInvalid == 1)
                .penaltiesSeconds(penalties > 0 ? penalties : null)
                .build();
    }
    
    private LapDataEvent buildEvent(PacketHeader header, LapDto payload) {
        return LapDataEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.LAP_DATA)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
