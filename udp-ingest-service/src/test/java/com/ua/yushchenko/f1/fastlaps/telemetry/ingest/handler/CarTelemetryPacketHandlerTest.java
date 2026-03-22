package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryBatchEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.config.CarTelemetryUdpProperties;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.CarTelemetryPacketParser;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarTelemetryPacketHandler")
class CarTelemetryPacketHandlerTest {

    @Mock
    private TelemetryPublisher publisher;
    @Mock
    private CarTelemetryUdpProperties carTelemetryUdpProperties;
    @Spy
    private CarTelemetryPacketParser carTelemetryPacketParser = new CarTelemetryPacketParser();
    @InjectMocks
    private CarTelemetryPacketHandler handler;

    @BeforeEach
    void defaultPlayerOnlyMode() {
        lenient().when(carTelemetryUdpProperties.getMode()).thenReturn(CarTelemetryUdpProperties.Mode.PLAYER_ONLY);
    }

    @Test
    @DisplayName("should parse and publish car telemetry")
    void shouldParseAndPublishCarTelemetry() {
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

        handler.handleCarTelemetryPacket(header, payload);

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

    @Test
    @DisplayName("BATCH_ALL_CARS publishes one CarTelemetryBatchEvent with 22 slots")
    void batchMode_publishesBatchWith22Slots() {
        lenient().when(carTelemetryUdpProperties.getMode()).thenReturn(CarTelemetryUdpProperties.Mode.BATCH_ALL_CARS);

        PacketHeader header = PacketHeader.builder()
                .packetFormat(2025)
                .gameYear((short) 25)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .packetVersion((short) 1)
                .packetId((short) 6)
                .sessionUID(999L)
                .sessionTime(10f)
                .frameIdentifier(42L)
                .overallFrameIdentifier(42L)
                .playerCarIndex((short) 3)
                .secondaryPlayerCarIndex((short) 0)
                .build();

        ByteBuffer payload = createFullGridCarTelemetryPayload();

        handler.handleCarTelemetryPacket(header, payload);

        ArgumentCaptor<CarTelemetryBatchEvent> captor = ArgumentCaptor.forClass(CarTelemetryBatchEvent.class);
        verify(publisher).publish(eq("telemetry.carTelemetry"), anyString(), captor.capture());
        CarTelemetryBatchEvent batch = captor.getValue();
        assertThat(batch.getSessionUID()).isEqualTo(999L);
        assertThat(batch.getFrameIdentifier()).isEqualTo(42);
        assertThat(batch.getCarIndex()).isEqualTo(3);
        assertThat(batch.getSamples()).hasSize(22);
        assertThat(batch.getSamples().get(0).getCarIndex()).isEqualTo(0);
        assertThat(batch.getSamples().get(3).getCarIndex()).isEqualTo(3);
        assertThat(batch.getSamples().get(3).getTelemetry().getSpeedKph()).isEqualTo(285);
    }

    private ByteBuffer createCarTelemetryPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(CarTelemetryPacketParser.CAR_TELEMETRY_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 285);
        buffer.putFloat(1.0f);
        buffer.putFloat(0.0f);
        buffer.putFloat(0.0f);
        buffer.put((byte) 0);
        buffer.put((byte) 7);
        buffer.putShort((short) 11500);
        buffer.put((byte) 1);
        buffer.put((byte) 75);
        buffer.putShort((short) 0);
        for (int i = 0; i < 4; i++) buffer.putShort((short) 400);
        for (int i = 0; i < 4; i++) buffer.put((byte) 100);
        for (int i = 0; i < 4; i++) buffer.put((byte) 105);
        buffer.putShort((short) 90);
        for (int i = 0; i < 4; i++) buffer.putFloat(23.0f);
        for (int i = 0; i < 4; i++) buffer.put((byte) 0);
        buffer.flip();
        return buffer;
    }

    private ByteBuffer createFullGridCarTelemetryPayload() {
        int stride = CarTelemetryPacketHandler.CAR_TELEMETRY_DATA_SIZE;
        ByteBuffer full = ByteBuffer.allocate(22 * stride).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer template = createCarTelemetryPayload();
        for (int car = 0; car < 22; car++) {
            template.rewind();
            full.put(template);
        }
        full.flip();
        return full;
    }
}
