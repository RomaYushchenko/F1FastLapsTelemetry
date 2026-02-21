package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.CarTelemetryPacketParser;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("CarTelemetryPacketHandler")
class CarTelemetryPacketHandlerTest {

    @Test
    @DisplayName("should parse and publish car telemetry")
    void shouldParseAndPublishCarTelemetry() {
        // Arrange
        TelemetryPublisher publisher = mock(TelemetryPublisher.class);
        CarTelemetryPacketHandler handler = new CarTelemetryPacketHandler(publisher, new CarTelemetryPacketParser());

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
        ByteBuffer buffer = ByteBuffer.allocate(1000).order(ByteOrder.LITTLE_ENDIAN);

        // Speed (kph)
        buffer.putShort((short) 285);
        // Throttle (0-1)
        buffer.putFloat(1.0f);
        // Steer (-1 to 1)
        buffer.putFloat(0.0f);
        // Brake (0-1)
        buffer.putFloat(0.0f);
        // Clutch
        buffer.put((byte) 0);
        // Gear
        buffer.put((byte) 7);
        // Engine RPM
        buffer.putShort((short) 11500);
        // DRS
        buffer.put((byte) 1);
        // Rev lights
        buffer.put((byte) 75);

        buffer.rewind();
        return buffer;
    }
}
