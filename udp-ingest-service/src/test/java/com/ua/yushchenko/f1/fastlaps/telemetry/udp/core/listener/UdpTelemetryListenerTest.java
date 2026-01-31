package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.listener;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher.UdpPacketDispatcher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UdpTelemetryListenerTest {
    
    private UdpTelemetryListener listener;
    
    @AfterEach
    void tearDown() {
        if (listener != null && listener.isRunning()) {
            listener.stop();
        }
    }
    
    @Test
    void shouldStartAndStopSuccessfully() throws IOException, InterruptedException {
        // Arrange
        UdpListenerConfig config = UdpListenerConfig.builder()
            .host("127.0.0.1")
            .port(0)  // Let OS choose available port
            .build();
        UdpPacketDispatcher dispatcher = mock(UdpPacketDispatcher.class);
        listener = new UdpTelemetryListener(config, dispatcher);
        
        // Act: Start in separate thread
        Thread listenerThread = new Thread(() -> {
            try {
                listener.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        listenerThread.start();
        
        // Wait for listener to start
        await(() -> listener.isRunning());
        
        // Assert
        assertThat(listener.isRunning()).isTrue();
        
        // Stop
        listener.stop();
        listenerThread.join(1000);
        assertThat(listener.isRunning()).isFalse();
    }
    
    @Test
    void shouldThrowExceptionWhenStartedTwice() throws IOException, InterruptedException {
        // Arrange
        UdpListenerConfig config = UdpListenerConfig.builder()
            .host("127.0.0.1")
            .port(0)
            .build();
        UdpPacketDispatcher dispatcher = mock(UdpPacketDispatcher.class);
        listener = new UdpTelemetryListener(config, dispatcher);
        
        Thread listenerThread = new Thread(() -> {
            try {
                listener.start();
            } catch (IOException e) {
                // Ignore
            }
        });
        listenerThread.start();
        await(() -> listener.isRunning());
        
        // Act & Assert: Starting again should throw exception
        assertThatThrownBy(() -> listener.start())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already running");
        
        // Cleanup
        listener.stop();
        listenerThread.join(1000);
    }
    
    @Test
    void shouldReceiveAndDispatchUdpPacket() throws Exception {
        // Arrange
        UdpListenerConfig config = UdpListenerConfig.builder()
            .host("127.0.0.1")
            .port(0)  // OS chooses port
            .build();
        
        UdpPacketDispatcher dispatcher = mock(UdpPacketDispatcher.class);
        listener = new UdpTelemetryListener(config, dispatcher);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> actualPort = new AtomicReference<>();
        
        // Start listener in separate thread
        Thread listenerThread = new Thread(() -> {
            try {
                listener.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        listenerThread.start();
        await(() -> listener.isRunning());
        
        // Get the actual port (requires reflection or config modification)
        // For this test, we'll use a fixed port
        listener.stop();
        listenerThread.join(1000);
        
        // Restart with known port
        config = UdpListenerConfig.builder()
            .host("127.0.0.1")
            .port(20888)  // Fixed test port
            .build();
        listener = new UdpTelemetryListener(config, dispatcher);
        
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(dispatcher).dispatch(any(), any());
        
        listenerThread = new Thread(() -> {
            try {
                listener.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        listenerThread.start();
        await(() -> listener.isRunning());
        
        // Act: Send UDP packet
        sendTestPacket("127.0.0.1", 20888, (short) 6);
        
        // Assert: Wait for dispatch
        boolean received = latch.await(2, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        
        ArgumentCaptor<PacketHeader> headerCaptor = ArgumentCaptor.forClass(PacketHeader.class);
        verify(dispatcher, atLeastOnce()).dispatch(headerCaptor.capture(), any());
        
        PacketHeader header = headerCaptor.getValue();
        assertThat(header.getPacketId()).isEqualTo((short) 6);
        
        // Cleanup
        listener.stop();
        listenerThread.join(1000);
    }
    
    @Test
    void shouldHandleInvalidPacketGracefully() throws Exception {
        // Arrange
        UdpListenerConfig config = UdpListenerConfig.builder()
            .host("127.0.0.1")
            .port(20889)
            .build();
        
        UdpPacketDispatcher dispatcher = mock(UdpPacketDispatcher.class);
        listener = new UdpTelemetryListener(config, dispatcher);
        
        Thread listenerThread = new Thread(() -> {
            try {
                listener.start();
            } catch (IOException e) {
                // Ignore
            }
        });
        listenerThread.start();
        await(() -> listener.isRunning());
        
        // Act: Send invalid packet (too small)
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] invalidData = new byte[]{1, 2, 3};  // Only 3 bytes
            DatagramPacket packet = new DatagramPacket(
                invalidData, invalidData.length,
                InetAddress.getByName("127.0.0.1"), 20889
            );
            socket.send(packet);
        }
        
        Thread.sleep(200);  // Give time to process
        
        // Assert: Should continue running despite invalid packet
        assertThat(listener.isRunning()).isTrue();
        
        // Cleanup
        listener.stop();
        listenerThread.join(1000);
    }
    
    private void sendTestPacket(String host, int port, short packetId) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // Valid F1 header
        buffer.putShort((short) 2025);
        buffer.put((byte) 25);
        buffer.put((byte) 1);
        buffer.put((byte) 0);
        buffer.put((byte) 1);
        buffer.put((byte) packetId);
        buffer.putLong(123456L);
        buffer.putFloat(10.0f);
        buffer.putInt(100);
        buffer.putInt(100);
        buffer.put((byte) 0);
        buffer.put((byte) 255);
        
        byte[] data = new byte[buffer.position()];
        buffer.flip();
        buffer.get(data);
        
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(
                data, data.length,
                InetAddress.getByName(host), port
            );
            socket.send(packet);
        }
    }
    
    private void await(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Condition not met within timeout");
    }
    
    @FunctionalInterface
    interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
