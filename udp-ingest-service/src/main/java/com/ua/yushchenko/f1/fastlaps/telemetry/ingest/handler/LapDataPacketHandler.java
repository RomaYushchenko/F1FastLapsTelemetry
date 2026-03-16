package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.LapDataEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.metrics.PacketLossMetricsRecorder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.LapDataPacketParser;
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
 * Handles Lap Data packets (packetId=2).
 * Parses lap times, sectors, and validity for all 22 cars and publishes to telemetry.lap topic.
 * Enables full leaderboard on Live Overview (positions and lap times for all cars).
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
    /** F1 25: 22 cars in lap data packet. */
    private static final int NUM_CARS = 22;

    private final TelemetryPublisher publisher;
    private final LapDataPacketParser lapDataPacketParser;
    private final PacketLossMetricsRecorder packetLossMetricsRecorder;

    @F1PacketHandler(packetId = 2)
    public void handleLapDataPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing lap data packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        int requiredBytes = NUM_CARS * LAP_DATA_SIZE;
        if (payload.remaining() < requiredBytes) {
            log.warn("Lap data payload too short: need {} bytes, have {}", requiredBytes, payload.remaining());
            packetLossMetricsRecorder.recordExpectedFrame(header.getSessionUID());
            packetLossMetricsRecorder.recordLostFrame(header.getSessionUID());
            return;
        }

        int startPosition = payload.position();
        try {
            packetLossMetricsRecorder.recordExpectedFrame(header.getSessionUID());
            for (int carIndex = 0; carIndex < NUM_CARS; carIndex++) {
                payload.position(startPosition + carIndex * LAP_DATA_SIZE);
                LapDto lapData = lapDataPacketParser.parse(payload);
                LapDataEvent event = LapDataEventBuilder.build(header, lapData, carIndex);
                String key = header.getSessionUID() + "-" + carIndex;
                publisher.publish(TOPIC, key, event);
            }
            packetLossMetricsRecorder.recordReceivedFrame(header.getSessionUID());
            log.trace("Published lap data for {} cars: sessionUID={}, frame={}", NUM_CARS,
                    header.getSessionUID(), header.getFrameIdentifier());
        } catch (Exception e) {
            log.error("Failed to parse lap data packet: sessionUID={}, frame={}",
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }
}
