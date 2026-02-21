package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.CarStatusEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.CarStatusPacketParser;
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
 * Handles Car Status packets (packetId=7).
 * Parses fuel, tyres, ERS, DRS status and publishes to telemetry.carStatus topic.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.status.enabled", havingValue = "true", matchIfMissing = true)
public class CarStatusPacketHandler {

    private static final String TOPIC = "telemetry.carStatus";
    /** CarStatusData size in bytes. From docs: (1239 - 29 header) / 22 cars = 55. */
    private static final int CAR_STATUS_DATA_SIZE = 55;

    private final TelemetryPublisher publisher;
    private final CarStatusPacketParser carStatusPacketParser;

    @F1PacketHandler(packetId = 7)
    public void handleCarStatusPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing car status packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        try {
            if (!seekToPlayerCar(payload, header.getPlayerCarIndex(), CAR_STATUS_DATA_SIZE, "car status")) {
                return;
            }

            CarStatusDto status = carStatusPacketParser.parse(payload);
            CarStatusEvent event = CarStatusEventBuilder.build(header, status);
            String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();

            publisher.publish(TOPIC, key, event);

            log.debug("Published car status: fuel={}, drs={}, ers={}",
                    status.getFuelInTank(), status.getDrsAllowed(), status.getErsStoreEnergy());
        } catch (Exception e) {
            log.error("Failed to parse car status packet: sessionUID={}, frame={}",
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
