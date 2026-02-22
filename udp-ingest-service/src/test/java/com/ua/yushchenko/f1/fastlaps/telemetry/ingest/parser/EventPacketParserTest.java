package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventPacketParser")
class EventPacketParserTest {

    private final EventPacketParser parser = new EventPacketParser();

    /** Build 45-byte payload: 4-byte code + 41 bytes union (rest zero). */
    private ByteBuffer payload(String code, int unionBytesToSet) {
        ByteBuffer buffer = ByteBuffer.allocate(EventPacketParser.EVENT_PAYLOAD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        byte[] codeBytes = new byte[4];
        for (int i = 0; i < Math.min(4, code.length()); i++) {
            codeBytes[i] = (byte) code.charAt(i);
        }
        buffer.put(codeBytes);
        for (int i = 0; i < unionBytesToSet; i++) {
            buffer.put((byte) 0);
        }
        buffer.flip();
        return buffer;
    }

    @Test
    @DisplayName("parse DRSD: eventCode and drsDisabledReason")
    void parseDrsd() {
        ByteBuffer buffer = ByteBuffer.allocate(EventPacketParser.EVENT_PAYLOAD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("DRSD".getBytes());
        buffer.put((byte) 1); // reason = Safety car
        while (buffer.position() < EventPacketParser.EVENT_PAYLOAD_SIZE) buffer.put((byte) 0);
        buffer.flip();

        EventDto dto = parser.parse(buffer);

        assertThat(dto.getEventCode()).isEqualTo("DRSD");
        assertThat(dto.getDrsDisabledReason()).isEqualTo(1);
    }

    @Test
    @DisplayName("parse SCAR: safetyCarType and safetyCarEventType")
    void parseScar() {
        ByteBuffer buffer = ByteBuffer.allocate(EventPacketParser.EVENT_PAYLOAD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("SCAR".getBytes());
        buffer.put((byte) 2); // Full SC = 1, VSC = 2
        buffer.put((byte) 0); // Deployed
        while (buffer.position() < EventPacketParser.EVENT_PAYLOAD_SIZE) buffer.put((byte) 0);
        buffer.flip();

        EventDto dto = parser.parse(buffer);

        assertThat(dto.getEventCode()).isEqualTo("SCAR");
        assertThat(dto.getSafetyCarType()).isEqualTo(2);
        assertThat(dto.getSafetyCarEventType()).isEqualTo(0);
    }

    @Test
    @DisplayName("parse RTMT: vehicleIdx and retirementReason")
    void parseRtmt() {
        ByteBuffer buffer = ByteBuffer.allocate(EventPacketParser.EVENT_PAYLOAD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RTMT".getBytes());
        buffer.put((byte) 5);  // vehicleIdx
        buffer.put((byte) 8);  // reason = mechanical failure
        while (buffer.position() < EventPacketParser.EVENT_PAYLOAD_SIZE) buffer.put((byte) 0);
        buffer.flip();

        EventDto dto = parser.parse(buffer);

        assertThat(dto.getEventCode()).isEqualTo("RTMT");
        assertThat(dto.getVehicleIdx()).isEqualTo(5);
        assertThat(dto.getRetirementReason()).isEqualTo(8);
    }

    @Test
    @DisplayName("parse FTLP: vehicleIdx and lapTime")
    void parseFtlp() {
        ByteBuffer buffer = ByteBuffer.allocate(EventPacketParser.EVENT_PAYLOAD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("FTLP".getBytes());
        buffer.put((byte) 0);  // vehicleIdx
        buffer.putFloat(88.123f); // lapTime seconds
        while (buffer.position() < EventPacketParser.EVENT_PAYLOAD_SIZE) buffer.put((byte) 0);
        buffer.flip();

        EventDto dto = parser.parse(buffer);

        assertThat(dto.getEventCode()).isEqualTo("FTLP");
        assertThat(dto.getVehicleIdx()).isEqualTo(0);
        assertThat(dto.getLapTime()).isEqualTo(88.123f);
    }

    @Test
    @DisplayName("parse advances buffer by 45 bytes")
    void parseAdvancesBufferBy45() {
        ByteBuffer buffer = ByteBuffer.allocate(EventPacketParser.EVENT_PAYLOAD_SIZE + 10).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("DRSD".getBytes());
        buffer.put((byte) 0);
        while (buffer.position() < 45) buffer.put((byte) 0);
        buffer.put((byte) 0x77); // extra
        buffer.flip();

        parser.parse(buffer);

        assertThat(buffer.position()).isEqualTo(45);
    }
}
