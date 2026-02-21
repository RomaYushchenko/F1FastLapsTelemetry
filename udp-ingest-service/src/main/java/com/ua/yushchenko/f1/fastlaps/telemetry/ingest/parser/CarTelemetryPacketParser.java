package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parses F1 25 CarTelemetryData from ByteBuffer into {@link CarTelemetryDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt (60 bytes per car).
 * Field order: speed, throttle, steer, brake, clutch, gear, engineRPM, drs, revLightsPercent, revLightsBitValue,
 * brakesTemperature[4], tyresSurfaceTemperature[4], tyresInnerTemperature[4], engineTemperature, tyresPressure[4], surfaceType[4].
 */
@Component
public class CarTelemetryPacketParser {

    /** Size in bytes of one CarTelemetryData struct (F1 25 spec). */
    public static final int CAR_TELEMETRY_DATA_SIZE_BYTES = 60;

    /**
     * Parse one car's telemetry from current buffer position. Buffer must be positioned at start of CarTelemetryData.
     * Advances buffer position by {@value #CAR_TELEMETRY_DATA_SIZE_BYTES} bytes.
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
        int revLightsPercent = buffer.get() & 0xFF;
        int revLightsBitValue = buffer.getShort() & 0xFFFF;
        int[] brakesTemperature = new int[4];
        for (int i = 0; i < 4; i++) {
            brakesTemperature[i] = buffer.getShort() & 0xFFFF;
        }
        int[] tyresSurfaceTemperature = new int[4];
        for (int i = 0; i < 4; i++) {
            tyresSurfaceTemperature[i] = buffer.get() & 0xFF;
        }
        int[] tyresInnerTemperature = new int[4];
        for (int i = 0; i < 4; i++) {
            tyresInnerTemperature[i] = buffer.get() & 0xFF;
        }
        int engineTemperature = buffer.getShort() & 0xFFFF;
        float[] tyresPressure = new float[4];
        for (int i = 0; i < 4; i++) {
            tyresPressure[i] = buffer.getFloat();
        }
        int[] surfaceType = new int[4];
        for (int i = 0; i < 4; i++) {
            surfaceType[i] = buffer.get() & 0xFF;
        }

        return CarTelemetryDto.builder()
                .speedKph(speed)
                .throttle(throttle)
                .brake(brake)
                .steer(steer)
                .gear(gear)
                .engineRpm(engineRPM)
                .drs(drs)
                .clutch(clutch)
                .revLightsPercent(revLightsPercent)
                .revLightsBitValue(revLightsBitValue)
                .brakesTemperature(brakesTemperature)
                .tyresSurfaceTemperature(tyresSurfaceTemperature)
                .tyresInnerTemperature(tyresInnerTemperature)
                .engineTemperature(engineTemperature)
                .tyresPressure(tyresPressure)
                .surfaceType(surfaceType)
                .build();
    }
}
