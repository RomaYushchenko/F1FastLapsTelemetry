package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.CarTelemetryPacketParser;
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
@DisplayName("CarTelemetryPacketHandler")
class CarTelemetryPacketHandlerTest {

    @Mock
    private TelemetryPublisher publisher;
    @Spy
    private CarTelemetryPacketParser carTelemetryPacketParser = new CarTelemetryPacketParser();
    @InjectMocks
    private CarTelemetryPacketHandler handler;

    @Test
    @DisplayName("should parse and publish car telemetry")
    void shouldParseAndPublishCarTelemetry() {
        // Arrange
        PacketHeader header = PacketHeader.builder()
                .packetFormat(2025)
                .gameYear((short) 25)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .packetVersion((short) 1)
                .packetId((short) 6)
                .sessionUID(123456789L)
                .sessionTime(125.5f)
                .frameIdentifier(1500L)
                .overallFrameIdentifier(1500L)
                .playerCarIndex((short) 0)
                .secondaryPlayerCarIndex((short) 0)
                .build();

        ByteBuffer payload = createCarTelemetryPayload();

        // Act
        handler.handleCarTelemetryPacket(header, payload);

        // Assert
        ArgumentCaptor<CarTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(CarTelemetryEvent.class);
        verify(publisher).publish(eq("telemetry.carTelemetry"), anyString(), eventCaptor.capture());

        CarTelemetryEvent event = eventCaptor.getValue();
        assertThat(event.getSessionUID()).isEqualTo(123456789L);
        assertThat(event.getPayload().getSpeedKph()).isEqualTo(285);
        assertThat(event.getPayload().getThrottle()).isEqualTo(1.0f);
        assertThat(event.getPayload().getBrake()).isEqualTo(0.0f);
        assertThat(event.getPayload().getGear()).isEqualTo(7);
        assertThat(event.getPayload().getEngineRpm()).isEqualTo(11500);
        assertThat(event.getPayload().getDrs()).isEqualTo(1);
    }

    private ByteBuffer createCarTelemetryPayload() {
        // Full 60 bytes of CarTelemetryData (F1 25) so parser advances correctly
        ByteBuffer buffer = ByteBuffer.allocate(CarTelemetryPacketParser.CAR_TELEMETRY_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 285);       // speed
        buffer.putFloat(1.0f);              // throttle
        buffer.putFloat(0.0f);              // steer
        buffer.putFloat(0.0f);              // brake
        buffer.put((byte) 0);               // clutch
        buffer.put((byte) 7);               // gear
        buffer.putShort((short) 11500);     // engineRPM
        buffer.put((byte) 1);               // drs
        buffer.put((byte) 75);              // revLightsPercent
        buffer.putShort((short) 0);         // revLightsBitValue
        for (int i = 0; i < 4; i++) buffer.putShort((short) 400);  // brakesTemperature
        for (int i = 0; i < 4; i++) buffer.put((byte) 100);        // tyresSurfaceTemperature
        for (int i = 0; i < 4; i++) buffer.put((byte) 105);       // tyresInnerTemperature
        buffer.putShort((short) 90);        // engineTemperature
        for (int i = 0; i < 4; i++) buffer.putFloat(23.0f);        // tyresPressure
        for (int i = 0; i < 4; i++) buffer.put((byte) 0);         // surfaceType
        buffer.flip();
        return buffer;
    }
}
