package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1PacketHandler;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1UdpListener;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Handles Car Damage packets (packetId=10).
 * Parses tyre wear (m_tyresWear[4]) and publishes to telemetry.carDamage topic.
 * Packet layout per docs: .github/docs/F1 25 Telemetry Output Structures.txt — CarDamageData (46 bytes per car).
 * Tyre order in UDP: index 0=RL, 1=RR, 2=FL, 3=FR.
 */
@Slf4j
@Component
@F1UdpListener
@RequiredArgsConstructor
@ConditionalOnProperty(name = "f1.telemetry.udp.handlers.damage.enabled", havingValue = "true", matchIfMissing = true)
public class CarDamagePacketHandler {

    private static final String TOPIC = "telemetry.carDamage";
    /** CarDamageData size in bytes. From docs: (1041 - 29 header) / 22 cars = 46. */
    private static final int CAR_DAMAGE_DATA_SIZE = 46;

    private final TelemetryPublisher publisher;

    @F1PacketHandler(packetId = 10)
    public void handleCarDamagePacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing car damage packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        try {
            int playerCarIndex = header.getPlayerCarIndex();

            int offset = playerCarIndex * CAR_DAMAGE_DATA_SIZE;
            if (payload.position() + offset + 16 > payload.limit()) {
                log.warn("Car damage payload too short for car index {}: need {} bytes", playerCarIndex, offset + 16);
                return;
            }
            payload.position(payload.position() + offset);

            CarDamageDto damage = parseTyreWear(payload);

            CarDamageEvent event = buildEvent(header, damage);
            String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();

            publisher.publish(TOPIC, key, event);

            log.debug("Published car damage: wear FL={}, FR={}, RL={}, RR={}",
                    damage.getTyresWearFL(), damage.getTyresWearFR(), damage.getTyresWearRL(), damage.getTyresWearRR());
        } catch (Exception e) {
            log.error("Failed to parse car damage packet: sessionUID={}, frame={}",
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }

    /**
     * Read m_tyresWear[4] (4 floats). F1 25 order: [0]=RL, [1]=RR, [2]=FL, [3]=FR.
     */
    private CarDamageDto parseTyreWear(ByteBuffer buffer) {
        float rl = buffer.getFloat();
        float rr = buffer.getFloat();
        float fl = buffer.getFloat();
        float fr = buffer.getFloat();
        return CarDamageDto.builder()
                .tyresWearRL(rl)
                .tyresWearRR(rr)
                .tyresWearFL(fl)
                .tyresWearFR(fr)
                .build();
    }

    private CarDamageEvent buildEvent(PacketHeader header, CarDamageDto payload) {
        return CarDamageEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.CAR_DAMAGE)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
