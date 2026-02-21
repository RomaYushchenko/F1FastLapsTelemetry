package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder.CarDamageEventBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.CarDamagePacketParser;
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
 * Handles Car Damage packets (packetId=10).
 * Parses tyre wear (m_tyresWear[4]) and publishes to telemetry.carDamage topic.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.damage.enabled", havingValue = "true", matchIfMissing = true)
public class CarDamagePacketHandler {

    private static final String TOPIC = "telemetry.carDamage";

    private final TelemetryPublisher publisher;
    private final CarDamagePacketParser carDamagePacketParser;

    @F1PacketHandler(packetId = 10)
    public void handleCarDamagePacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing car damage packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        try {
            if (!seekToPlayerCar(payload, header.getPlayerCarIndex(),
                    CarDamagePacketParser.CAR_DAMAGE_DATA_SIZE_BYTES, "car damage")) {
                return;
            }

            CarDamageDto damage = carDamagePacketParser.parse(payload);
            CarDamageEvent event = CarDamageEventBuilder.build(header, damage);
            String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();

            publisher.publish(TOPIC, key, event);

            log.debug("Published car damage: wear FL={}, FR={}, RL={}, RR={}",
                    damage.getTyresWearFL(), damage.getTyresWearFR(), damage.getTyresWearRL(), damage.getTyresWearRR());
        } catch (Exception e) {
            log.error("Failed to parse car damage packet: sessionUID={}, frame={}",
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
