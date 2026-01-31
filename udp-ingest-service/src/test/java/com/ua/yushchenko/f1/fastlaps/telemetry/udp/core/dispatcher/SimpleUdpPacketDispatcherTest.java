package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SimpleUdpPacketDispatcherTest {
    
    private SimpleUdpPacketDispatcher dispatcher;
    
    @BeforeEach
    void setUp() {
        dispatcher = new SimpleUdpPacketDispatcher();
    }
    
    @Test
    void shouldDispatchToRegisteredConsumer() {
        // Arrange
        UdpPacketConsumer consumer = mock(UdpPacketConsumer.class);
        when(consumer.packetId()).thenReturn((short) 1);
        dispatcher.registerConsumer(consumer);
        
        PacketHeader header = createHeader((short) 1);
        ByteBuffer payload = ByteBuffer.allocate(10);
        
        // Act
        dispatcher.dispatch(header, payload);
        
        // Assert
        verify(consumer, times(1)).handle(eq(header), any(ByteBuffer.class));
    }
    
    @Test
    void shouldDispatchToMultipleConsumersForSamePacketId() {
        // Arrange
        UdpPacketConsumer consumer1 = mock(UdpPacketConsumer.class);
        UdpPacketConsumer consumer2 = mock(UdpPacketConsumer.class);
        when(consumer1.packetId()).thenReturn((short) 6);
        when(consumer2.packetId()).thenReturn((short) 6);
        
        dispatcher.registerConsumer(consumer1);
        dispatcher.registerConsumer(consumer2);
        
        PacketHeader header = createHeader((short) 6);
        ByteBuffer payload = ByteBuffer.allocate(10);
        
        // Act
        dispatcher.dispatch(header, payload);
        
        // Assert
        verify(consumer1).handle(eq(header), any(ByteBuffer.class));
        verify(consumer2).handle(eq(header), any(ByteBuffer.class));
    }
    
    @Test
    void shouldNotDispatchToConsumersOfDifferentPacketId() {
        // Arrange
        UdpPacketConsumer consumer1 = mock(UdpPacketConsumer.class);
        UdpPacketConsumer consumer2 = mock(UdpPacketConsumer.class);
        when(consumer1.packetId()).thenReturn((short) 1);
        when(consumer2.packetId()).thenReturn((short) 2);
        
        dispatcher.registerConsumer(consumer1);
        dispatcher.registerConsumer(consumer2);
        
        PacketHeader header = createHeader((short) 1);
        ByteBuffer payload = ByteBuffer.allocate(10);
        
        // Act
        dispatcher.dispatch(header, payload);
        
        // Assert
        verify(consumer1).handle(eq(header), any(ByteBuffer.class));
        verify(consumer2, never()).handle(any(), any());
    }
    
    @Test
    void shouldRewindBufferForEachConsumer() {
        // Arrange
        List<Integer> positions = new ArrayList<>();
        
        UdpPacketConsumer consumer1 = new UdpPacketConsumer() {
            @Override
            public short packetId() { return 1; }
            
            @Override
            public void handle(PacketHeader header, ByteBuffer payload) {
                positions.add(payload.position());
                payload.get(); // Advance position
            }
        };
        
        UdpPacketConsumer consumer2 = new UdpPacketConsumer() {
            @Override
            public short packetId() { return 1; }
            
            @Override
            public void handle(PacketHeader header, ByteBuffer payload) {
                positions.add(payload.position());
            }
        };
        
        dispatcher.registerConsumer(consumer1);
        dispatcher.registerConsumer(consumer2);
        
        PacketHeader header = createHeader((short) 1);
        ByteBuffer payload = ByteBuffer.allocate(10);
        
        // Act
        dispatcher.dispatch(header, payload);
        
        // Assert: Both consumers should see position 0
        assertThat(positions).containsExactly(0, 0);
    }
    
    @Test
    void shouldContinueDispatchingWhenConsumerThrowsException() {
        // Arrange
        UdpPacketConsumer faultyConsumer = mock(UdpPacketConsumer.class);
        UdpPacketConsumer goodConsumer = mock(UdpPacketConsumer.class);
        
        when(faultyConsumer.packetId()).thenReturn((short) 1);
        when(goodConsumer.packetId()).thenReturn((short) 1);
        
        doThrow(new RuntimeException("Consumer error"))
            .when(faultyConsumer).handle(any(), any());
        
        dispatcher.registerConsumer(faultyConsumer);
        dispatcher.registerConsumer(goodConsumer);
        
        PacketHeader header = createHeader((short) 1);
        ByteBuffer payload = ByteBuffer.allocate(10);
        
        // Act: Should not throw exception
        dispatcher.dispatch(header, payload);
        
        // Assert: Good consumer should still be invoked
        verify(goodConsumer).handle(eq(header), any(ByteBuffer.class));
    }
    
    @Test
    void shouldHandleDispatchWithNoRegisteredConsumers() {
        // Arrange
        PacketHeader header = createHeader((short) 99);
        ByteBuffer payload = ByteBuffer.allocate(10);
        
        // Act & Assert: Should not throw exception
        dispatcher.dispatch(header, payload);
    }
    
    private PacketHeader createHeader(short packetId) {
        return PacketHeader.builder()
            .packetFormat(2025)
            .gameYear((short) 25)
            .gameMajorVersion((short) 1)
            .gameMinorVersion((short) 0)
            .packetVersion((short) 1)
            .packetId(packetId)
            .sessionUID(12345L)
            .sessionTime(10.0f)
            .frameIdentifier(100L)
            .overallFrameIdentifier(100L)
            .playerCarIndex((short) 0)
            .secondaryPlayerCarIndex((short) 255)
            .build();
    }
}
