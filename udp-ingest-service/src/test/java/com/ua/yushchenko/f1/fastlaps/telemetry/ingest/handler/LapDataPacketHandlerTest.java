package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.LapDataPacketParser;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LapDataPacketHandler")
class LapDataPacketHandlerTest {

    @Mock
    private TelemetryPublisher publisher;
    @Spy
    private LapDataPacketParser lapDataPacketParser = new LapDataPacketParser();
    @InjectMocks
    private LapDataPacketHandler handler;

    @Test
    @DisplayName("should parse and publish lap data for all 22 cars")
    void shouldParseAndPublishLapData() {
        // Arrange
        PacketHeader header = PacketHeader.builder()
                .packetFormat(2025)
                .gameYear((short) 25)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .packetVersion((short) 1)
                .packetId((short) 2)
                .sessionUID(123456789L)
                .sessionTime(125.5f)
                .frameIdentifier(1500L)
                .overallFrameIdentifier(1500L)
                .playerCarIndex((short) 0)
                .secondaryPlayerCarIndex((short) 0)
                .build();

        ByteBuffer payload = createFullLapDataPayload22Cars();

        // Act
        handler.handleLapDataPacket(header, payload);

        // Assert: 22 events published (one per car)
        ArgumentCaptor<LapDataEvent> eventCaptor = ArgumentCaptor.forClass(LapDataEvent.class);
        verify(publisher, org.mockito.Mockito.times(22)).publish(eq("telemetry.lap"), anyString(), eventCaptor.capture());

        LapDataEvent eventCar0 = eventCaptor.getAllValues().get(0);
        assertThat(eventCar0.getSessionUID()).isEqualTo(123456789L);
        assertThat(eventCar0.getCarIndex()).isEqualTo(0);
        assertThat(eventCar0.getPlayerCarIndex()).isEqualTo(0);
        assertThat(eventCar0.getPayload().getLapNumber()).isEqualTo(5);
        assertThat(eventCar0.getPayload().getLastLapTimeMs()).isEqualTo(84500);
        assertThat(eventCar0.getPayload().getCurrentLapTimeMs()).isEqualTo(85500);
        assertThat(eventCar0.getPayload().getSector1TimeMs()).isEqualTo(28500);
        assertThat(eventCar0.getPayload().getSector2TimeMs()).isEqualTo(30000);
        assertThat(eventCar0.getPayload().isInvalid()).isFalse();
    }

    @Test
    @DisplayName("should detect invalid lap")
    void shouldDetectInvalidLap() {
        // Arrange
        PacketHeader header = PacketHeader.builder()
                .packetFormat(2025)
                .gameYear((short) 25)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .packetVersion((short) 1)
                .packetId((short) 2)
                .sessionUID(123456789L)
                .sessionTime(125.5f)
                .frameIdentifier(1500L)
                .overallFrameIdentifier(1500L)
                .playerCarIndex((short) 0)
                .secondaryPlayerCarIndex((short) 0)
                .build();

        ByteBuffer payload = createFullInvalidLapDataPayload22Cars();

        // Act
        handler.handleLapDataPacket(header, payload);

        // Assert: car 0 event has invalid lap
        ArgumentCaptor<LapDataEvent> eventCaptor = ArgumentCaptor.forClass(LapDataEvent.class);
        verify(publisher, org.mockito.Mockito.times(22)).publish(eq("telemetry.lap"), anyString(), eventCaptor.capture());
        assertThat(eventCaptor.getAllValues().get(0).getPayload().isInvalid()).isTrue();
    }

    /** One car's lap data (57 bytes). */
    private ByteBuffer createLapDataPayload() {
        // Full 57 bytes of LapData (F1 25) per LapDataPacketParser.LAP_DATA_SIZE_BYTES
        ByteBuffer buffer = ByteBuffer.allocate(LapDataPacketParser.LAP_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(84500);
        buffer.putInt(85500);
        buffer.putShort((short) 28500);
        buffer.put((byte) 0);
        buffer.putShort((short) 30000);
        buffer.put((byte) 0);
        buffer.putShort((short) 0);
        buffer.put((byte) 0);
        buffer.putShort((short) 0);
        buffer.put((byte) 0);
        buffer.putFloat(2500.5f);
        buffer.putFloat(15000.0f);
        buffer.putFloat(0.0f);
        buffer.put((byte) 3);
        buffer.put((byte) 5);
        buffer.put((byte) 0);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 0);           // Valid
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 2);          // gridPosition
        buffer.put((byte) 4);          // driverStatus
        buffer.put((byte) 2);          // resultStatus
        buffer.put((byte) 0);          // pitLaneTimerActive
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.put((byte) 0);
        buffer.putFloat(0.0f);
        buffer.put((byte) 255);        // speedTrapFastestLap not set
        buffer.flip();
        return buffer;
    }

    /** Full payload: 22 cars × 57 bytes (car 0 = valid lap data, rest = same). */
    private ByteBuffer createFullLapDataPayload22Cars() {
        ByteBuffer full = ByteBuffer.allocate(22 * LapDataPacketParser.LAP_DATA_SIZE_BYTES);
        for (int i = 0; i < 22; i++) {
            full.put(createLapDataPayload().duplicate());
        }
        full.flip();
        return full;
    }

    private ByteBuffer createInvalidLapDataPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(LapDataPacketParser.LAP_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(84500);
        buffer.putInt(85500);
        buffer.putShort((short) 28500);
        buffer.put((byte) 0);
        buffer.putShort((short) 30000);
        buffer.put((byte) 0);
        buffer.putShort((short) 0);
        buffer.put((byte) 0);
        buffer.putShort((short) 0);
        buffer.put((byte) 0);
        buffer.putFloat(2500.5f);
        buffer.putFloat(15000.0f);
        buffer.putFloat(0.0f);
        buffer.put((byte) 3);
        buffer.put((byte) 5);
        buffer.put((byte) 0);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 1);           // Invalid
        buffer.put((byte) 5);           // 5 seconds penalty
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 2);
        buffer.put((byte) 4);
        buffer.put((byte) 2);
        buffer.put((byte) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.put((byte) 0);
        buffer.putFloat(0.0f);
        buffer.put((byte) 255);
        buffer.flip();
        return buffer;
    }

    /** Full payload: 22 cars × 57 bytes (car 0 = invalid lap, rest = zeros). */
    private ByteBuffer createFullInvalidLapDataPayload22Cars() {
        ByteBuffer full = ByteBuffer.allocate(22 * LapDataPacketParser.LAP_DATA_SIZE_BYTES);
        full.put(createInvalidLapDataPayload().duplicate());
        for (int i = 1; i < 22; i++) {
            full.put(createLapDataPayload().duplicate());
        }
        full.flip();
        return full;
    }
}
