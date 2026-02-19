package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.KafkaEnvelope;
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
 * Handles Car Telemetry packets (packetId=6).
 * Parses speed, throttle, brake, gear, RPM and publishes to telemetry.carTelemetry topic.
 * Packet layout per docs: .github/docs/F1 25 Telemetry Output Structures.txt — CarTelemetryData (58 bytes per car).
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
    
    @F1PacketHandler(packetId = 6)
    public void handleCarTelemetryPacket(PacketHeader header, ByteBuffer payload) {
        log.debug("Processing car telemetry packet: sessionUID={}, frame={}", 
                header.getSessionUID(), header.getFrameIdentifier());
        
        try {
            int playerCarIndex = header.getPlayerCarIndex();
            
            // Skip to player car data (payload is already positioned at start of car array after header)
            int offset = playerCarIndex * CAR_TELEMETRY_DATA_SIZE;
            if (payload.position() + offset + CAR_TELEMETRY_DATA_SIZE > payload.limit()) {
                log.warn("Car telemetry payload too short for car index {}: need {} bytes", playerCarIndex, offset + CAR_TELEMETRY_DATA_SIZE);
                return;
            }
            payload.position(payload.position() + offset);
            
            CarTelemetryDto telemetry = parseCarTelemetry(payload);
            
            KafkaEnvelope<CarTelemetryDto> envelope = buildEnvelope(header, telemetry);
            String key = header.getSessionUID() + "-" + header.getPlayerCarIndex();
            
            publisher.publish(TOPIC, key, envelope);
            
            log.trace("Published car telemetry: speed={}kph, gear={}, rpm={}", 
                    telemetry.getSpeedKph(), telemetry.getGear(), telemetry.getEngineRpm());
        } catch (Exception e) {
            log.error("Failed to parse car telemetry packet: sessionUID={}, frame={}", 
                    header.getSessionUID(), header.getFrameIdentifier(), e);
        }
    }
    
    private CarTelemetryDto parseCarTelemetry(ByteBuffer buffer) {
        // F1 25 CarTelemetryData — docs: .github/docs/F1 25 Telemetry Output Structures.txt
        int speed = buffer.getShort() & 0xFFFF;         // uint16 — speed in km/h
        float throttle = buffer.getFloat();             // float 0.0–1.0
        float steer = buffer.getFloat();                 // float -1.0–1.0
        float brake = buffer.getFloat();                 // float 0.0–1.0
        int clutch = buffer.get() & 0xFF;                // uint8
        int gear = buffer.get();                         // int8 (1–8, N=0, R=-1)
        int engineRPM = buffer.getShort() & 0xFFFF;      // uint16
        int drs = buffer.get() & 0xFF;                   // uint8 0=off 1=on
        int revLightsPercent = buffer.get() & 0xFF;     // uint8
        buffer.getShort();                               // uint16 revLightsBitValue — skip
        for (int i = 0; i < 4; i++) buffer.getShort();   // uint16[4] brakesTemperature
        for (int i = 0; i < 4; i++) buffer.get();        // uint8[4] tyresSurfaceTemperature
        for (int i = 0; i < 4; i++) buffer.get();        // uint8[4] tyresInnerTemperature
        buffer.getShort();                               // uint16 engineTemperature
        for (int i = 0; i < 4; i++) buffer.getFloat();   // float[4] tyresPressure
        for (int i = 0; i < 4; i++) buffer.get();        // uint8[4] surfaceType

        return CarTelemetryDto.builder()
                .speedKph(speed)
                .throttle(throttle)
                .brake(brake)
                .steer(steer)
                .gear(gear)
                .engineRpm(engineRPM)
                .drs(drs)
                .build();
    }
    
    private KafkaEnvelope<CarTelemetryDto> buildEnvelope(PacketHeader header, CarTelemetryDto payload) {
        return KafkaEnvelope.<CarTelemetryDto>builder()
                .schemaVersion(1)
                .packetId(PacketId.CAR_TELEMETRY)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
