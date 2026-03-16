package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantsEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.ParticipantsEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.ParticipantsPacketParser;
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
 * Handles Participants packets (packetId=4). Parses participant list (race number, name per car) and publishes to telemetry.participants topic.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.participants.enabled", havingValue = "true", matchIfMissing = true)
public class ParticipantsPacketHandler {

    private static final String TOPIC = "telemetry.participants";
    private static final int REQUIRED_PAYLOAD_BYTES = 1 + ParticipantsPacketParser.NUM_CARS * ParticipantsPacketParser.PARTICIPANT_DATA_SIZE_BYTES;

    private final TelemetryPublisher publisher;
    private final ParticipantsPacketParser participantsPacketParser;

    @F1PacketHandler(packetId = 4)
    public void handleParticipantsPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing participants packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        if (payload.remaining() < REQUIRED_PAYLOAD_BYTES) {
            log.warn("Participants payload too short: need {} bytes, have {}", REQUIRED_PAYLOAD_BYTES, payload.remaining());
            return;
        }

        try {
            ParticipantsDto dto = participantsPacketParser.parse(payload);
            ParticipantsEvent event = ParticipantsEventBuilder.build(header, dto);
            String key = String.valueOf(header.getSessionUID());
            publisher.publish(TOPIC, key, event);
            log.debug("Published participants: sessionUID={}, numActiveCars={}",
                    header.getSessionUID(), dto.getNumActiveCars());
        } catch (Exception e) {
            log.error("Failed to parse participants packet: sessionUID={}, frame={}",
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }
}
