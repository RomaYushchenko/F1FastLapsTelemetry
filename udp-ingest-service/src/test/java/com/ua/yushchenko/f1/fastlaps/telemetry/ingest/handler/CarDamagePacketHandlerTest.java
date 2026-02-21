package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.CarDamagePacketParser;
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
@DisplayName("CarDamagePacketHandler")
class CarDamagePacketHandlerTest {

    @Mock
    private TelemetryPublisher publisher;
    @Spy
    private CarDamagePacketParser carDamagePacketParser = new CarDamagePacketParser();
    @InjectMocks
    private CarDamagePacketHandler handler;

    @Test
    @DisplayName("should parse and publish car damage")
    void shouldParseAndPublishCarDamage() {
        // Arrange
        PacketHeader header = PacketHeader.builder()
                .packetFormat(2025)
                .gameYear((short) 25)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .packetVersion((short) 1)
                .packetId((short) 10)
                .sessionUID(123456789L)
                .sessionTime(125.5f)
                .frameIdentifier(1500L)
                .overallFrameIdentifier(1500L)
                .playerCarIndex((short) 0)
                .secondaryPlayerCarIndex((short) 0)
                .build();

        ByteBuffer payload = createCarDamagePayload();

        // Act
        handler.handleCarDamagePacket(header, payload);

        // Assert
        ArgumentCaptor<CarDamageEvent> eventCaptor = ArgumentCaptor.forClass(CarDamageEvent.class);
        verify(publisher).publish(eq("telemetry.carDamage"), anyString(), eventCaptor.capture());

        CarDamageEvent event = eventCaptor.getValue();
        assertThat(event.getSessionUID()).isEqualTo(123456789L);
        assertThat(event.getPayload().getTyresWearRL()).isEqualTo(0.12f);
        assertThat(event.getPayload().getTyresWearRR()).isEqualTo(0.15f);
        assertThat(event.getPayload().getTyresWearFL()).isEqualTo(0.08f);
        assertThat(event.getPayload().getTyresWearFR()).isEqualTo(0.10f);
    }

    private ByteBuffer createCarDamagePayload() {
        // Full 46 bytes of CarDamageData (F1 25) for player car 0 per CarDamagePacketParser.CAR_DAMAGE_DATA_SIZE_BYTES
        ByteBuffer buffer = ByteBuffer.allocate(CarDamagePacketParser.CAR_DAMAGE_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(0.12f);  // tyresWear RL, RR, FL, FR
        buffer.putFloat(0.15f);
        buffer.putFloat(0.08f);
        buffer.putFloat(0.10f);
        for (int i = 0; i < 4; i++) buffer.put((byte) 0);   // tyresDamage
        for (int i = 0; i < 4; i++) buffer.put((byte) 0);   // brakesDamage
        for (int i = 0; i < 4; i++) buffer.put((byte) 0);   // tyreBlisters
        for (int i = 0; i < 18; i++) buffer.put((byte) 0);  // single-byte damage fields
        buffer.flip();
        return buffer;
    }
}
