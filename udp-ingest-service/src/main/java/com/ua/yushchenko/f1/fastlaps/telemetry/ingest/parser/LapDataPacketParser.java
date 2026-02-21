package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parses F1 25 LapData from ByteBuffer into {@link LapDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt (57 bytes per car).
 */
@Component
public class LapDataPacketParser {

    /**
     * Parse one car's lap data from current buffer position. Buffer must be positioned at start of LapData.
     * Advances buffer position by full LapData size.
     */
    public LapDto parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int lastLapTimeMs = buffer.getInt();
        int currentLapTimeMs = buffer.getInt();
        int sector1MsPart = buffer.getShort() & 0xFFFF;
        int sector1MinPart = buffer.get() & 0xFF;
        int sector2MsPart = buffer.getShort() & 0xFFFF;
        int sector2MinPart = buffer.get() & 0xFF;
        int sector1TimeMs = sector1MinPart * 60_000 + sector1MsPart;
        int sector2TimeMs = sector2MinPart * 60_000 + sector2MsPart;
        buffer.getShort();
        buffer.get();
        buffer.getShort();
        buffer.get();
        float lapDistance = buffer.getFloat();
        buffer.getFloat();
        buffer.getFloat();
        buffer.get();
        int currentLapNum = buffer.get() & 0xFF;
        buffer.get();
        buffer.get();
        int sector = buffer.get() & 0xFF;
        int currentLapInvalid = buffer.get() & 0xFF;
        int penalties = buffer.get() & 0xFF;
        buffer.get();
        buffer.get();
        buffer.get();
        buffer.get();
        buffer.get();
        buffer.get();
        buffer.get();
        buffer.get();
        buffer.getShort();
        buffer.getShort();
        buffer.get();
        buffer.getFloat();
        buffer.get();

        return LapDto.builder()
                .lapNumber(currentLapNum)
                .lapDistance(lapDistance)
                .lastLapTimeMs(lastLapTimeMs > 0 ? lastLapTimeMs : null)
                .currentLapTimeMs(currentLapTimeMs > 0 ? currentLapTimeMs : null)
                .sector1TimeMs(sector1TimeMs > 0 ? sector1TimeMs : null)
                .sector2TimeMs(sector2TimeMs > 0 ? sector2TimeMs : null)
                .sector(sector)
                .isInvalid(currentLapInvalid == 1)
                .penaltiesSeconds(penalties > 0 ? penalties : null)
                .build();
    }
}
