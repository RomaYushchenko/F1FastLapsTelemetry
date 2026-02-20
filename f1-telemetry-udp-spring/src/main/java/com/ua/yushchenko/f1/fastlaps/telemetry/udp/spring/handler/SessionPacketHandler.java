package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionLifecycleEvent;
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
 * Handles Session packets (packetId=1).
 * Parses session start/end events and publishes to telemetry.session topic.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.session.enabled", havingValue = "true", matchIfMissing = true)
public class SessionPacketHandler {
    
    private static final String TOPIC = "telemetry.session";
    
    private final TelemetryPublisher publisher;
    
    @F1PacketHandler(packetId = 1)
    public void handleSessionPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing session packet: sessionUID={}, frame={}", 
                header.getSessionUID(), header.getFrameIdentifier());
        
        try {
            SessionEventDto sessionEvent = parseSessionPacket(payload);
            EventCode eventCode = sessionEvent.getEventCode();

            if (eventCode == EventCode.SSTA || eventCode == EventCode.SEND) {
                SessionLifecycleEvent event = buildEvent(header, sessionEvent);
                String key = String.valueOf(header.getSessionUID());
                publisher.publish(TOPIC, key, event);
                log.info("Published session event: eventCode={}, sessionType={}, sessionUID={}",
                        eventCode, sessionEvent.getSessionType(), header.getSessionUID());
            } else if (eventCode == EventCode.SESSION_TIMEOUT
                    && (sessionEvent.getSessionTypeId() != null || sessionEvent.getTrackId() != null)) {
                // Publish session metadata for unknown events that carry type/track (not for FLBK etc.)
                SessionEventDto infoPayload = SessionEventDto.builder()
                        .eventCode(EventCode.SESSION_INFO)
                        .sessionType(sessionEvent.getSessionType())
                        .sessionTypeId(sessionEvent.getSessionTypeId())
                        .trackId(sessionEvent.getTrackId())
                        .totalLaps(sessionEvent.getTotalLaps())
                        .build();
                SessionLifecycleEvent event = buildEvent(header, infoPayload);
                String key = String.valueOf(header.getSessionUID());
                publisher.publish(TOPIC, key, event);
                log.debug("Published SESSION_INFO: sessionType={}, trackId={}, sessionUID={}",
                        infoPayload.getSessionType(), infoPayload.getTrackId(), header.getSessionUID());
            }
        } catch (Exception e) {
            log.error("Failed to parse session packet: sessionUID={}, frame={}", 
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }
    
    private SessionEventDto parseSessionPacket(ByteBuffer buffer) {
        // F1 2025 Session packet structure (simplified for MVP)
        // Byte 0: Event code (4 char string like "SSTA", "SEND")
        byte[] eventCodeBytes = new byte[4];
        buffer.get(eventCodeBytes);
        String eventCodeStr = new String(eventCodeBytes).trim();
        
        EventCode eventCode = parseEventCode(eventCodeStr);
        
        // Skip to session type field (offset varies, using typical position)
        buffer.position(buffer.position() + 20); // Skip event details
        byte sessionTypeId = buffer.get();
        String sessionType = parseSessionType(sessionTypeId);
        
        // Track ID (1 byte)
        int trackId = Byte.toUnsignedInt(buffer.get());
        
        // Total laps (1 byte)
        int totalLaps = Byte.toUnsignedInt(buffer.get());
        
        return SessionEventDto.builder()
                .eventCode(eventCode)
                .sessionType(sessionType)
                .sessionTypeId(Byte.toUnsignedInt(sessionTypeId))
                .trackId(trackId)
                .totalLaps(totalLaps)
                .build();
    }
    
    private EventCode parseEventCode(String code) {
        return switch (code) {
            case "SSTA" -> EventCode.SSTA;
            case "SEND" -> EventCode.SEND;
            case "FLBK" -> EventCode.FLBK;
            default -> EventCode.SESSION_TIMEOUT;
        };
    }
    
    private String parseSessionType(byte sessionTypeId) {
        return switch (sessionTypeId) {
            case 0 -> "UNKNOWN";
            case 1 -> "PRACTICE_1";
            case 2 -> "PRACTICE_2";
            case 3 -> "PRACTICE_3";
            case 4 -> "SHORT_PRACTICE";
            case 5 -> "QUALIFYING_1";
            case 6 -> "QUALIFYING_2";
            case 7 -> "QUALIFYING_3";
            case 8 -> "SHORT_QUALIFYING";
            case 9 -> "ONE_SHOT_QUALIFYING";
            case 10 -> "RACE";
            case 11 -> "RACE_2";
            case 12 -> "TIME_TRIAL";
            default -> "UNKNOWN";
        };
    }
    
    private SessionLifecycleEvent buildEvent(PacketHeader header, SessionEventDto payload) {
        return SessionLifecycleEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.SESSION)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
