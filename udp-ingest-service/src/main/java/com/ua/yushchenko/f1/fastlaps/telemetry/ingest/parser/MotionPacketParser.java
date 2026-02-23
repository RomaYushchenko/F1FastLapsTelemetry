package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.MotionDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parses F1 25 CarMotionData from ByteBuffer into {@link MotionDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt: 60 bytes per car.
 * Order: worldPosition X,Y,Z (12), worldVelocity X,Y,Z (12), 6x int16 (12), gForceLateral,Longitudinal,Vertical (12), yaw,pitch,roll (12).
 */
@Component
public class MotionPacketParser {

    /** Size in bytes of one CarMotionData struct (F1 25 spec). */
    public static final int CAR_MOTION_DATA_SIZE_BYTES = 60;

    /**
     * Parse one car's motion from current buffer position. Buffer must be positioned at start of CarMotionData.
     * Advances buffer position by {@value #CAR_MOTION_DATA_SIZE_BYTES} bytes.
     */
    public MotionDto parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        float worldPositionX = buffer.getFloat();
        float worldPositionY = buffer.getFloat();
        float worldPositionZ = buffer.getFloat();
        buffer.position(buffer.position() + 12); // worldVelocity
        buffer.position(buffer.position() + 12); // worldForwardDir + worldRightDir (6 int16)
        float gForceLateral = buffer.getFloat();
        buffer.getFloat(); // gForceLongitudinal
        buffer.getFloat(); // gForceVertical
        float yaw = buffer.getFloat();
        return new MotionDto(worldPositionX, worldPositionY, worldPositionZ, gForceLateral, yaw);
    }
}
