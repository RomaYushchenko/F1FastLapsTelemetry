package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.F1SessionType;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionLifecycleEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.SessionDataEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.SessionLifecycleEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.SessionDataPacketParser;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.SessionPacketParser;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1PacketHandler;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1UdpListener;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

/**
 * Handles Session packets (packetId=1).
 * If payload is full PacketSessionData (724 bytes), parses and publishes to telemetry.sessionData.
 * Otherwise parses as event-like (SSTA/SEND) and publishes to telemetry.session.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.session.enabled", havingValue = "true", matchIfMissing = true)
public class SessionPacketHandler {

    private static final String TOPIC_SESSION = "telemetry.session";
    private static final String TOPIC_SESSION_DATA = "telemetry.sessionData";

    private final TelemetryPublisher publisher;
    private final SessionPacketParser sessionPacketParser;
    private final SessionDataPacketParser sessionDataPacketParser;

    private static final int MIN_EVENT_PAYLOAD_SIZE = 4 + 20 + 3; // eventCode(4) + skip(20) + sessionType+trackId+totalLaps(3)

    @F1PacketHandler(packetId = 1)
    public void handleSessionPacket(PacketHeader header, ByteBuffer payload) {
        int remaining = payload.remaining();
        log.debug("handleSessionPacket: sessionUID={}, frame={}, payloadRemaining={}",
                header.getSessionUID(), header.getFrameIdentifier(), remaining);

        try {
            if (remaining >= SessionDataPacketParser.SESSION_DATA_PAYLOAD_SIZE) {
                log.debug("Session packet treated as full SessionData (payloadRemaining={} >= {}), publishing to {}",
                        remaining, SessionDataPacketParser.SESSION_DATA_PAYLOAD_SIZE, TOPIC_SESSION_DATA);
                handleFullSessionData(header, payload);
                return;
            }

            if (remaining < MIN_EVENT_PAYLOAD_SIZE) {
                log.warn("Session payload too short for event parse: need {} bytes, have {} (sessionUID={}, frame={})",
                        MIN_EVENT_PAYLOAD_SIZE, remaining, header.getSessionUID(), header.getFrameIdentifier());
                return;
            }

            log.debug("Session packet treated as event (payloadRemaining={} < {}), parsing event",
                    remaining, SessionDataPacketParser.SESSION_DATA_PAYLOAD_SIZE);
            SessionEventDto sessionEvent = sessionPacketParser.parse(payload);
            EventCode eventCode = sessionEvent.getEventCode();

            if (eventCode == EventCode.SSTA || eventCode == EventCode.SEND) {
                publishSessionEvent(header, sessionEvent);
                log.info("Published session event: eventCode={}, sessionType={}, trackId={}, totalLaps={}, sessionUID={}",
                        eventCode, F1SessionType.fromCode(sessionEvent.getSessionTypeId()).getDisplayName(),
                        sessionEvent.getTrackId(), sessionEvent.getTotalLaps(), header.getSessionUID());
            } else if (eventCode == EventCode.SESSION_TIMEOUT
                    && (sessionEvent.getSessionTypeId() != null || sessionEvent.getTrackId() != null)) {
                SessionEventDto infoPayload = SessionEventDto.builder()
                        .eventCode(EventCode.SESSION_INFO)
                        .sessionTypeId(sessionEvent.getSessionTypeId())
                        .trackId(sessionEvent.getTrackId())
                        .totalLaps(sessionEvent.getTotalLaps())
                        .build();
                publishSessionEvent(header, infoPayload);
                log.info("Published SESSION_INFO: sessionTypeId={}, trackId={}, totalLaps={}, sessionUID={} (track layout can use trackId)",
                        infoPayload.getSessionTypeId(), infoPayload.getTrackId(), infoPayload.getTotalLaps(), header.getSessionUID());
            } else {
                log.debug("Session packet not published: eventCode={}, sessionTypeId={}, trackId={}, sessionUID={} (not SSTA/SEND, no SESSION_INFO metadata)",
                        eventCode, sessionEvent.getSessionTypeId(), sessionEvent.getTrackId(), header.getSessionUID());
            }
        } catch (Exception e) {
            log.error("Failed to parse session packet: sessionUID={}, frame={}, payloadRemaining={}",
                    header.getSessionUID(), header.getFrameIdentifier(), remaining, e);
        }
    }

    private void handleFullSessionData(PacketHeader header, ByteBuffer payload) {
        var sessionData = sessionDataPacketParser.parse(payload);
        SessionDataEvent event = SessionDataEventBuilder.build(header, sessionData);
        String key = String.valueOf(header.getSessionUID());
        publisher.publish(TOPIC_SESSION_DATA, key, event);
        log.info("Session data published: topic={}, sessionUID={}, trackId={}, totalLaps={} (track layout recording uses this)",
                TOPIC_SESSION_DATA, header.getSessionUID(), sessionData.getTrackId(), sessionData.getTotalLaps());
    }

    private void publishSessionEvent(PacketHeader header, SessionEventDto payload) {
        SessionLifecycleEvent event = SessionLifecycleEventBuilder.build(header, payload);
        String key = String.valueOf(header.getSessionUID());
        publisher.publish(TOPIC_SESSION, key, event);
    }
}
