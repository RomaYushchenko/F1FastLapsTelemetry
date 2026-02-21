package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.CarTelemetryEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.CarTelemetryPacketParser;
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
 * Handles Car Telemetry packets (packetId=6).
 * Parses speed, throttle, brake, gear, RPM and publishes to telemetry.carTelemetry topic.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.telemetry.enabled", havingValue = "true", matchIfMissing = true)
public class CarTelemetryPacketHandler {

    private static final String TOPIC = "telemetry.carTelemetry";
    /** CarTelemetryData size in bytes. From docs: (1352 - 29 header - 3 trailer) / 22 cars = 60. */
    private static final int CAR_TELEMETRY_DATA_SIZE = 60;

    private final TelemetryPublisher publisher;
    private final CarTelemetryPacketParser carTelemetryPacketParser;

    @F1PacketHandler(packetId = 6)
    public void handleCarTelemetryPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing car telemetry packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        try {
            if (!seekToPlayerCar(payload, header.getPlayerCarIndex(), CAR_TELEMETRY_DATA_SIZE, "car telemetry")) {
                return;
            }

            CarTelemetryDto telemetry = carTelemetryPacketParser.parse(payload);
            CarTelemetryEvent event = CarTelemetryEventBuilder.build(header, telemetry);
            String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();

            publisher.publish(TOPIC, key, event);

            log.trace("Published car telemetry: speed={}kph, gear={}, rpm={}",
                    telemetry.getSpeedKph(), telemetry.getGear(), telemetry.getEngineRpm());
        } catch (Exception e) {
            log.error("Failed to parse car telemetry packet: sessionUID={}, frame={}",
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
