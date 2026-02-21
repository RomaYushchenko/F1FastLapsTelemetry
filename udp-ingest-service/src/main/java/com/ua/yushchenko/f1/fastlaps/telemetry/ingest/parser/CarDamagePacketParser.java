package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

/**
 * Parses F1 25 CarDamageData (m_tyresWear[4]) from ByteBuffer into {@link CarDamageDto}.
 * Tyre order in UDP: index 0=RL, 1=RR, 2=FL, 3=FR.
 */
@Component
public class CarDamagePacketParser {

    /**
     * Read m_tyresWear[4] (4 floats) from current buffer position.
     * Buffer must have at least 16 bytes remaining.
     */
    public CarDamageDto parse(ByteBuffer buffer) {
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
}
