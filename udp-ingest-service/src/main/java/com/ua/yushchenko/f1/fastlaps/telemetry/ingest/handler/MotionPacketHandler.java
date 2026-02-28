package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.MotionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.MotionEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.MotionEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.MotionPacketParser;
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
 * Handles Motion packets (packetId=0). Parses player car motion and publishes to telemetry.motion topic.
 * Plan: 13-session-summary-speed-corner-graph.md Phase 4.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.motion.enabled", havingValue = "true", matchIfMissing = true)
public class MotionPacketHandler {

    private static final String TOPIC = "telemetry.motion";

    private final TelemetryPublisher publisher;
    private final MotionPacketParser motionPacketParser;

    @F1PacketHandler(packetId = 0)
    public void handleMotionPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing motion packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        try {
            if (!seekToPlayerCar(payload, header.getPlayerCarIndex(), "motion")) {
                return;
            }

            MotionDto motion = motionPacketParser.parse(payload);
            MotionEvent event = MotionEventBuilder.build(header, motion);
            String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();

            publisher.publish(TOPIC, key, event);

            log.trace("Published motion: posX={}, posZ={}, gLat={}, yaw={}",
                    motion.getWorldPositionX(), motion.getWorldPositionZ(),
                    motion.getGForceLateral(), motion.getYaw());
        } catch (Exception e) {
            log.error("Failed to parse motion packet: sessionUID={}, frame={}",
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }

    private boolean seekToPlayerCar(ByteBuffer payload, int playerCarIndex, String packetType) {
        int dataSizePerCar = MotionPacketParser.CAR_MOTION_DATA_SIZE_BYTES;
        int offset = playerCarIndex * dataSizePerCar;
        if (payload.position() + offset + dataSizePerCar > payload.limit()) {
            log.warn("{} payload too short for car index {}: need {} bytes", packetType, playerCarIndex, offset + dataSizePerCar);
            return false;
        }
        payload.position(payload.position() + offset);
        return true;
    }
}
