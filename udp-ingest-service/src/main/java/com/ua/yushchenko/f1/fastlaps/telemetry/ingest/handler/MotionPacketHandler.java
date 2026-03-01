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
 * Handles Motion packets (packetId=0). Parses motion for all 22 cars and publishes to telemetry.motion topic (B9).
 * Plan: 13-session-summary-speed-corner-graph.md Phase 4; block-h multi-car positions.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.motion.enabled", havingValue = "true", matchIfMissing = true)
public class MotionPacketHandler {

    private static final String TOPIC = "telemetry.motion";
    /** F1 25: 22 cars in motion packet. */
    private static final int NUM_CARS = 22;

    private final TelemetryPublisher publisher;
    private final MotionPacketParser motionPacketParser;

    @F1PacketHandler(packetId = 0)
    public void handleMotionPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing motion packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        int dataSizePerCar = MotionPacketParser.CAR_MOTION_DATA_SIZE_BYTES;
        int requiredBytes = NUM_CARS * dataSizePerCar;
        if (payload.remaining() < requiredBytes) {
            log.warn("Motion payload too short: need {} bytes, have {}", requiredBytes, payload.remaining());
            return;
        }

        int startPosition = payload.position();
        try {
            for (int carIndex = 0; carIndex < NUM_CARS; carIndex++) {
                payload.position(startPosition + carIndex * dataSizePerCar);
                MotionDto motion = motionPacketParser.parse(payload);
                MotionEvent event = MotionEventBuilder.build(header, motion, carIndex);
                String key = header.getSessionUID() + "-" + carIndex;
                publisher.publish(TOPIC, key, event);
            }
            log.trace("Published motion for {} cars: sessionUID={}, frame={}", NUM_CARS,
                    header.getSessionUID(), header.getFrameIdentifier());
        } catch (Exception e) {
            log.error("Failed to parse motion packet: sessionUID={}, frame={}",
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }
}
