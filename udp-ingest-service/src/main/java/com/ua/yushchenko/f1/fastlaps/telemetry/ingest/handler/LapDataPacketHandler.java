package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.LapDataEventBuilder;
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
 * Parses lap times, sectors, and validity and publishes to telemetry.lap topic.
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
    private final LapDataPacketParser lapDataPacketParser;

    @F1PacketHandler(packetId = 2)
    public void handleLapDataPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing lap data packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        try {
            if (!seekToPlayerCar(payload, header.getPlayerCarIndex(), LAP_DATA_SIZE, "lap data")) {
                return;
            }

            LapDto lapData = lapDataPacketParser.parse(payload);
            LapDataEvent event = LapDataEventBuilder.build(header, lapData);
            String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();

            publisher.publish(TOPIC, key, event);

            log.debug("Published lap data: lap={}, time={}ms, invalid={}",
                    lapData.getLapNumber(), lapData.getCurrentLapTimeMs(), lapData.isInvalid());
        } catch (Exception e) {
            log.error("Failed to parse lap data packet: sessionUID={}, frame={}",
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }

    private boolean seekToPlayerCar(ByteBuffer payload, int playerCarIndex, int dataSizePerCar, String packetType) {
        int offset = playerCarIndex * dataSizePerCar;
        if (payload.position() + offset + dataSizePerCar > payload.limit()) {
            log.warn("{} payload too short for car index {}: need {} bytes", packetType, playerCarIndex, offset + dataSizePerCar);
            return false;
        }
        payload.position(payload.position() + offset);
        return true;
    }
}
