package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusEvent;
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

class CarStatusPacketHandlerTest {
    
    @Test
    void shouldParseAndPublishCarStatus() {
        // Given
        TelemetryPublisher publisher = mock(TelemetryPublisher.class);
        CarStatusPacketHandler handler = new CarStatusPacketHandler(publisher);
        
        PacketHeader header = PacketHeader.builder()
                .packetFormat(2025)
                .gameYear((short) 25)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .packetVersion((short) 1)
                .packetId((short) 7)
                .sessionUID(123456789L)
                .sessionTime(125.5f)
                .frameIdentifier(1500L)
                .overallFrameIdentifier(1500L)
                .playerCarIndex((short) 0)
                .secondaryPlayerCarIndex((short) 0)
                .build();
        
        ByteBuffer payload = createCarStatusPayload();
        
        // When
        handler.handleCarStatusPacket(header, payload);
        
        // Then
        ArgumentCaptor<CarStatusEvent> eventCaptor = ArgumentCaptor.forClass(CarStatusEvent.class);
        verify(publisher).publish(eq("telemetry.carStatus"), anyString(), eventCaptor.capture());
        
        CarStatusEvent event = eventCaptor.getValue();
        assertThat(event.getSessionUID()).isEqualTo(123456789L);
        assertThat(event.getPayload().getTractionControl()).isEqualTo(1);
        assertThat(event.getPayload().getAbs()).isEqualTo(1);
        assertThat(event.getPayload().getFuelInTank()).isEqualTo(45.5f);
        assertThat(event.getPayload().getFuelMix()).isEqualTo(2);
        assertThat(event.getPayload().getDrsAllowed()).isTrue();
        assertThat(event.getPayload().getTyresCompound()).isEqualTo(16);
        assertThat(event.getPayload().getErsStoreEnergy()).isEqualTo(2500000.0f);
    }
    
    private ByteBuffer createCarStatusPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(1000).order(ByteOrder.LITTLE_ENDIAN);
        
        // Traction control
        buffer.put((byte) 1);
        // ABS
        buffer.put((byte) 1);
        // Fuel mix
        buffer.put((byte) 2);
        // Front brake bias
        buffer.put((byte) 55);
        // Pit limiter
        buffer.put((byte) 0);
        // Fuel in tank
        buffer.putFloat(45.5f);
        // Fuel capacity
        buffer.putFloat(110.0f);
        // Fuel remaining laps
        buffer.putFloat(15.2f);
        // Max RPM
        buffer.putShort((short) 12000);
        // Idle RPM
        buffer.putShort((short) 7000);
        // Max gears
        buffer.put((byte) 8);
        // DRS allowed
        buffer.put((byte) 1);
        // DRS activation distance
        buffer.putShort((short) 0);
        // Actual tyre compound
        buffer.put((byte) 16);
        // Visual tyre compound
        buffer.put((byte) 16);
        // Tyres age laps
        buffer.put((byte) 5);
        // Vehicle FIA flags
        buffer.put((byte) 0);
        // Engine power ICE
        buffer.putFloat(750000.0f);
        // Engine power MGUK
        buffer.putFloat(120000.0f);
        // ERS store energy
        buffer.putFloat(2500000.0f);
        
        buffer.rewind();
        return buffer;
    }
}
