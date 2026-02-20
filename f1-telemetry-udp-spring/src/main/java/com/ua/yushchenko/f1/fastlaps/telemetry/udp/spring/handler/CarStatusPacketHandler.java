package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusEvent;
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
 * Handles Car Status packets (packetId=7).
 * Parses fuel, tyres, ERS, DRS status and publishes to telemetry.carStatus topic.
 * Packet layout per docs: .github/docs/F1 25 Telemetry Output Structures.txt — CarStatusData (55 bytes per car).
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

    @F1PacketHandler(packetId = 7)
    public void handleCarStatusPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing car status packet: sessionUID={}, frame={}",
                header.getSessionUID(), header.getFrameIdentifier());

        try {
            int playerCarIndex = header.getPlayerCarIndex();

            // Payload starts after 29-byte header; skip to player car data
            int offset = playerCarIndex * CAR_STATUS_DATA_SIZE;
            if (payload.position() + offset + CAR_STATUS_DATA_SIZE > payload.limit()) {
                log.warn("Car status payload too short for car index {}: need {} bytes", playerCarIndex, offset + CAR_STATUS_DATA_SIZE);
                return;
            }
            payload.position(payload.position() + offset);

            CarStatusDto status = parseCarStatus(payload);
            
            CarStatusEvent event = buildEvent(header, status);
            String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();
            
            publisher.publish(TOPIC, key, event);
            
            log.debug("Published car status: fuel={}, drs={}, ers={}", 
                    status.getFuelInTank(), status.getDrsAllowed(), status.getErsStoreEnergy());
        } catch (Exception e) {
            log.error("Failed to parse car status packet: sessionUID={}, frame={}", 
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }
    
    private CarStatusDto parseCarStatus(ByteBuffer buffer) {
        // F1 25 CarStatusData — docs: .github/docs/F1 25 Telemetry Output Structures.txt
        int tractionControl = buffer.get() & 0xFF;      // uint8 (0 = off, 1-2 = level)
        int antiLockBrakes = buffer.get() & 0xFF;       // uint8 (0 = off, 1 = on)
        int fuelMix = buffer.get() & 0xFF;              // uint8
        int frontBrakeBias = buffer.get() & 0xFF;       // uint8
        int pitLimiterStatus = buffer.get() & 0xFF;     // uint8
        float fuelInTank = buffer.getFloat();           // float
        float fuelCapacity = buffer.getFloat();         // float
        float fuelRemainingLaps = buffer.getFloat();    // float
        int maxRPM = buffer.getShort() & 0xFFFF;        // uint16
        int idleRPM = buffer.getShort() & 0xFFFF;       // uint16
        int maxGears = buffer.get() & 0xFF;             // uint8
        int drsAllowed = buffer.get() & 0xFF;           // uint8 (0 = not allowed, 1 = allowed)
        int drsActivationDistance = buffer.getShort() & 0xFFFF; // uint16
        int actualTyreCompound = buffer.get() & 0xFF;   // uint8
        int visualTyreCompound = buffer.get() & 0xFF;   // uint8
        int tyresAgeLaps = buffer.get() & 0xFF;         // uint8
        int vehicleFiaFlags = buffer.get();             // int8
        float enginePowerICE = buffer.getFloat();       // float
        float enginePowerMGUK = buffer.getFloat();      // float
        float ersStoreEnergy = buffer.getFloat();       // float (in Joules)
        // Skip rest of CarStatusData: ersDeployMode, ersHarvested*, ersDeployed*, networkPaused (14 bytes)
        if (buffer.remaining() >= 14) {
            buffer.position(buffer.position() + 14);
        }

        return CarStatusDto.builder()
                .tractionControl(tractionControl)
                .abs(antiLockBrakes)
                .fuelInTank(fuelInTank)
                .fuelMix(fuelMix)
                .drsAllowed(drsAllowed == 1)
                .tyresCompound(actualTyreCompound)
                .tyresAgeLaps(tyresAgeLaps)
                .ersStoreEnergy(ersStoreEnergy)
                .build();
    }
    
    private CarStatusEvent buildEvent(PacketHeader header, CarStatusDto payload) {
        return CarStatusEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.CAR_STATUS)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
