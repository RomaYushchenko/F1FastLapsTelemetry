package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CarDamagePacketHandlerTest {

    @Test
    void shouldParseAndPublishCarDamage() {
        TelemetryPublisher publisher = mock(TelemetryPublisher.class);
        CarDamagePacketHandler handler = new CarDamagePacketHandler(publisher);

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

        handler.handleCarDamagePacket(header, payload);

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
        ByteBuffer buffer = ByteBuffer.allocate(22 * 46).order(ByteOrder.LITTLE_ENDIAN);
        // For car index 0: m_tyresWear[4] = RL, RR, FL, FR (4 floats = 16 bytes)
        buffer.putFloat(0.12f);  // RL
        buffer.putFloat(0.15f);  // RR
        buffer.putFloat(0.08f);  // FL
        buffer.putFloat(0.10f);  // FR
        // Pad rest of first car's 46 bytes (30 more bytes)
        for (int i = 0; i < 30; i++) {
            buffer.put((byte) 0);
        }
        // Pad remaining 21 cars
        for (int c = 0; c < 21; c++) {
            for (int i = 0; i < 46; i++) {
                buffer.put((byte) 0);
            }
        }
        buffer.rewind();
        return buffer;
    }
}
