package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryBatchEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetrySlotDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.CarTelemetryBatchEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.CarTelemetryEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.config.CarTelemetryUdpProperties;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Handles Car Telemetry packets (packetId=6).
 * Publishes either one legacy {@link CarTelemetryEvent} (player only) or one batched
 * {@link CarTelemetryBatchEvent} for all grid slots to telemetry.carTelemetry.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.telemetry.enabled", havingValue = "true", matchIfMissing = true)
public class CarTelemetryPacketHandler {

    private static final String TOPIC = "telemetry.carTelemetry";
    /** CarTelemetryData size in bytes. From docs: (1352 - 29 header - 3 trailer) / 22 cars = 60. */
    static final int CAR_TELEMETRY_DATA_SIZE = 60;
    /** F1 grid slots per Car Telemetry packet. */
    private static final int CAR_COUNT = 22;

    private final TelemetryPublisher publisher;
    private final CarTelemetryPacketParser carTelemetryPacketParser;
    private final CarTelemetryUdpProperties carTelemetryUdpProperties;

    @F1PacketHandler(packetId = 6)
    public void handleCarTelemetryPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing car telemetry packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        try {
            if (carTelemetryUdpProperties.getMode() == CarTelemetryUdpProperties.Mode.BATCH_ALL_CARS) {
                publishBatchAllCars(header, payload);
            } else {
                publishPlayerOnly(header, payload);
            }
        } catch (Exception e) {
            log.error("Failed to parse car telemetry packet: sessionUID={}, frame={}",
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }

    private void publishPlayerOnly(PacketHeader header, ByteBuffer payload) {
        if (!seekToPlayerCar(payload, header.getPlayerCarIndex(), CAR_TELEMETRY_DATA_SIZE, "car telemetry")) {
            return;
        }
        CarTelemetryDto telemetry = carTelemetryPacketParser.parse(payload);
        CarTelemetryEvent event = CarTelemetryEventBuilder.build(header, telemetry);
        String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();
        publisher.publish(TOPIC, key, event);
        log.trace("Published car telemetry (player): speed={}kph, gear={}, rpm={}",
                telemetry.getSpeedKph(), telemetry.getGear(), telemetry.getEngineRpm());
    }

    private void publishBatchAllCars(PacketHeader header, ByteBuffer payload) {
        int base = payload.position();
        int need = CAR_COUNT * CAR_TELEMETRY_DATA_SIZE;
        if (payload.remaining() < need) {
            log.warn("Car telemetry payload too short for {} cars: remaining={}, need={}",
                    CAR_COUNT, payload.remaining(), need);
            return;
        }
        List<CarTelemetrySlotDto> samples = new ArrayList<>(CAR_COUNT);
        for (int carIdx = 0; carIdx < CAR_COUNT; carIdx++) {
            payload.position(base + carIdx * CAR_TELEMETRY_DATA_SIZE);
            CarTelemetryDto telemetry = carTelemetryPacketParser.parse(payload);
            samples.add(CarTelemetrySlotDto.builder()
                    .carIndex(carIdx)
                    .telemetry(telemetry)
                    .build());
        }
        CarTelemetryBatchEvent batch = CarTelemetryBatchEventBuilder.build(header, samples);
        String key = header.getSessionUID() + "-" + header.getFrameIdentifier();
        publisher.publish(TOPIC, key, batch);
        log.trace("Published car telemetry batch: sessionUID={}, frame={}, slots={}",
                header.getSessionUID(), header.getFrameIdentifier(), samples.size());
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
