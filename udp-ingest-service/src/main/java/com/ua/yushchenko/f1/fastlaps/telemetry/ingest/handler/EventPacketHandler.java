package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.EventEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.EventPacketParser;
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
 * Handles Packet Event (packetId=3). Parses event code and details, publishes to telemetry.event topic.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.event.enabled", havingValue = "true", matchIfMissing = true)
public class EventPacketHandler {

    private static final String TOPIC = "telemetry.event";

    private final TelemetryPublisher publisher;
    private final EventPacketParser eventPacketParser;

    @F1PacketHandler(packetId = 3)
    public void handleEventPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing event packet: sessionUID={}, frame={}", header.getSessionUID(), header.getFrameIdentifier());

        if (payload.remaining() < EventPacketParser.EVENT_PAYLOAD_SIZE) {
            log.warn("Event payload too short: need {} bytes, got {}", EventPacketParser.EVENT_PAYLOAD_SIZE, payload.remaining());
            return;
        }

        try {
            EventDto eventDto = eventPacketParser.parse(payload);
            EventEvent event = EventEventBuilder.build(header, eventDto);
            String key = header.getSessionUID() + "-" + header.getFrameIdentifier();

            publisher.publish(TOPIC, key, event);

            log.debug("Published event: code={}, sessionUID={}", eventDto.getEventCode(), header.getSessionUID());
        } catch (Exception e) {
            log.error("Failed to parse event packet: sessionUID={}, frame={}",
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }
}
