package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parses F1 25 CarTelemetryData from ByteBuffer into {@link CarTelemetryDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt (60 bytes per car).
 */
@Component
public class CarTelemetryPacketParser {

    /**
     * Parse one car's telemetry from current buffer position. Buffer must be positioned at start of CarTelemetryData.
     * Advances buffer position by full CarTelemetryData size.
     */
    public CarTelemetryDto parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int speed = buffer.getShort() & 0xFFFF;
        float throttle = buffer.getFloat();
        float steer = buffer.getFloat();
        float brake = buffer.getFloat();
        int clutch = buffer.get() & 0xFF;
        int gear = buffer.get();
        int engineRPM = buffer.getShort() & 0xFFFF;
        int drs = buffer.get() & 0xFF;
        buffer.get(); // revLightsPercent
        buffer.getShort(); // revLightsBitValue
        for (int i = 0; i < 4; i++) buffer.getShort(); // brakesTemperature
        for (int i = 0; i < 4; i++) buffer.get();      // tyresSurfaceTemperature
        for (int i = 0; i < 4; i++) buffer.get();      // tyresInnerTemperature
        buffer.getShort(); // engineTemperature
        for (int i = 0; i < 4; i++) buffer.getFloat(); // tyresPressure
        for (int i = 0; i < 4; i++) buffer.get();      // surfaceType

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
}
