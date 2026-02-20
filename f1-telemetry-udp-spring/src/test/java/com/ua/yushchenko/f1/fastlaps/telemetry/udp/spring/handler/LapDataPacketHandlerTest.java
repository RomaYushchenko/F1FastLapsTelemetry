package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LapDataPacketHandlerTest {
    
    @Test
    void shouldParseAndPublishLapData() {
        // Given
        TelemetryPublisher publisher = mock(TelemetryPublisher.class);
        LapDataPacketHandler handler = new LapDataPacketHandler(publisher);
        
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
        
        ByteBuffer payload = createLapDataPayload();
        
        // When
        handler.handleLapDataPacket(header, payload);
        
        // Then
        ArgumentCaptor<LapDataEvent> eventCaptor = ArgumentCaptor.forClass(LapDataEvent.class);
        verify(publisher).publish(eq("telemetry.lap"), anyString(), eventCaptor.capture());
        
        LapDataEvent event = eventCaptor.getValue();
        assertThat(event.getSessionUID()).isEqualTo(123456789L);
        assertThat(event.getPayload().getLapNumber()).isEqualTo(5);
        assertThat(event.getPayload().getCurrentLapTimeMs()).isEqualTo(85500);
        assertThat(event.getPayload().isInvalid()).isFalse();
    }
    
    @Test
    void shouldDetectInvalidLap() {
        // Given
        TelemetryPublisher publisher = mock(TelemetryPublisher.class);
        LapDataPacketHandler handler = new LapDataPacketHandler(publisher);
        
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
        
        ByteBuffer payload = createInvalidLapDataPayload();
        
        // When
        handler.handleLapDataPacket(header, payload);
        
        // Then
        ArgumentCaptor<LapDataEvent> eventCaptor = ArgumentCaptor.forClass(LapDataEvent.class);
        verify(publisher).publish(eq("telemetry.lap"), anyString(), eventCaptor.capture());
        
        LapDataEvent event = eventCaptor.getValue();
        assertThat(event.getPayload().isInvalid()).isTrue();
    }
    
    private ByteBuffer createLapDataPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(1000).order(ByteOrder.LITTLE_ENDIAN);
        
        // Last lap time
        buffer.putInt(84500);           // 84.5 seconds
        // Current lap time
        buffer.putInt(85500);           // 85.5 seconds
        // Sector 1 time
        buffer.putShort((short) 28500); // 28.5 seconds
        buffer.put((byte) 0);           // minutes
        // Sector 2 time
        buffer.putShort((short) 30000); // 30 seconds
        buffer.put((byte) 0);           // minutes
        // Lap distance
        buffer.putFloat(2500.5f);
        // Total distance
        buffer.putFloat(15000.0f);
        // Safety car delta
        buffer.putFloat(0.0f);
        // Car position
        buffer.put((byte) 3);
        // Current lap number
        buffer.put((byte) 5);
        // Pit status
        buffer.put((byte) 0);
        // Num pit stops
        buffer.put((byte) 1);
        // Sector
        buffer.put((byte) 2);
        // Current lap invalid
        buffer.put((byte) 0);           // Valid
        // Penalties
        buffer.put((byte) 0);
        // Warnings
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        
        buffer.rewind();
        return buffer;
    }
    
    private ByteBuffer createInvalidLapDataPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(1000).order(ByteOrder.LITTLE_ENDIAN);
        
        buffer.putInt(84500);
        buffer.putInt(85500);
        buffer.putShort((short) 28500);
        buffer.put((byte) 0);
        buffer.putShort((short) 30000);
        buffer.put((byte) 0);
        buffer.putFloat(2500.5f);
        buffer.putFloat(15000.0f);
        buffer.putFloat(0.0f);
        buffer.put((byte) 3);
        buffer.put((byte) 5);
        buffer.put((byte) 0);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 1);           // Invalid!
        buffer.put((byte) 5);           // 5 seconds penalty
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        
        buffer.rewind();
        return buffer;
    }
}
