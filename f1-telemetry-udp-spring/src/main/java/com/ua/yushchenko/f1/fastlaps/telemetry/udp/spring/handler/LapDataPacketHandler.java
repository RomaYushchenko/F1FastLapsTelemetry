package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.KafkaEnvelope;
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
import java.time.Instant;

/**
 * Handles Lap Data packets (packetId=2).
 * Parses lap times, sectors, and validity and publishes to telemetry.lap topic.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.lap.enabled", havingValue = "true", matchIfMissing = true)
public class LapDataPacketHandler {
    
    private static final String TOPIC = "telemetry.lap";
    private static final int LAP_DATA_SIZE = 53; // Bytes per car in F1 2025
    
    private final TelemetryPublisher publisher;
    
    @F1PacketHandler(packetId = 2)
    public void handleLapDataPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing lap data packet: sessionUID={}, frame={}", 
                header.getSessionUID(), header.getFrameIdentifier());
        
        try {
            int playerCarIndex = header.getPlayerCarIndex();
            
            // Skip to player car data
            payload.position(playerCarIndex * LAP_DATA_SIZE);
            
            LapDto lapData = parseLapData(payload);
            
            KafkaEnvelope<LapDto> envelope = buildEnvelope(header, lapData);
            String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();
            
            publisher.publish(TOPIC, key, envelope);
            
            log.debug("Published lap data: lap={}, time={}ms, invalid={}", 
                    lapData.getLapNumber(), lapData.getCurrentLapTimeMs(), lapData.isInvalid());
        } catch (Exception e) {
            log.error("Failed to parse lap data packet: sessionUID={}, frame={}", 
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }
    
    private LapDto parseLapData(ByteBuffer buffer) {
        // F1 2025 Lap Data structure (per car)
        int lastLapTimeMs = buffer.getInt();           // uint32
        int currentLapTimeMs = buffer.getInt();        // uint32
        int sector1TimeMs = buffer.getShort() & 0xFFFF; // uint16
        int sector1TimeMinutes = buffer.get() & 0xFF;   // uint8
        int sector2TimeMs = buffer.getShort() & 0xFFFF; // uint16
        int sector2TimeMinutes = buffer.get() & 0xFF;   // uint8
        float lapDistance = buffer.getFloat();          // float
        float totalDistance = buffer.getFloat();        // float
        float safetyCarDelta = buffer.getFloat();       // float
        int carPosition = buffer.get() & 0xFF;          // uint8
        int currentLapNum = buffer.get() & 0xFF;        // uint8
        int pitStatus = buffer.get() & 0xFF;            // uint8
        int numPitStops = buffer.get() & 0xFF;          // uint8
        int sector = buffer.get() & 0xFF;               // uint8
        int currentLapInvalid = buffer.get() & 0xFF;    // uint8 (0 = valid, 1 = invalid)
        int penalties = buffer.get() & 0xFF;            // uint8 (accumulated time penalties in seconds)
        int totalWarnings = buffer.get() & 0xFF;        // uint8
        int cornerCuttingWarnings = buffer.get() & 0xFF; // uint8
        int numUnservedDriveThroughPens = buffer.get() & 0xFF; // uint8
        int numUnservedStopGoPens = buffer.get() & 0xFF; // uint8
        
        return LapDto.builder()
                .lapNumber(currentLapNum)
                .lapDistance(lapDistance)
                .currentLapTimeMs(currentLapTimeMs > 0 ? currentLapTimeMs : null)
                .sector(sector)
                .isInvalid(currentLapInvalid == 1)
                .penaltiesSeconds(penalties > 0 ? penalties : null)
                .build();
    }
    
    private KafkaEnvelope<LapDto> buildEnvelope(PacketHeader header, LapDto payload) {
        return KafkaEnvelope.<LapDto>builder()
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
