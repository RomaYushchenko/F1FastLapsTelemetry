package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.listener;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher.UdpPacketDispatcher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeaderDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP telemetry listener that receives F1 game packets.
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>Create with configuration and dispatcher</li>
 *   <li>Call {@link #start()} to begin receiving packets</li>
 *   <li>Call {@link #stop()} to gracefully shutdown</li>
 * </ol>
 * <p>
 * The listener runs in a blocking loop on the calling thread.
 * For non-blocking operation, start in a separate thread.
 */
@Slf4j
public class UdpTelemetryListener {
    
    private static final int MAX_PACKET_SIZE = 2048;
    
    private final UdpListenerConfig config;
    private final UdpPacketDispatcher dispatcher;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private DatagramChannel channel;
    
    public UdpTelemetryListener(UdpListenerConfig config, UdpPacketDispatcher dispatcher) {
        this.config = config;
        this.dispatcher = dispatcher;
    }
    
    /**
     * Starts the UDP listener.
     * <p>
     * Binds to configured host and port, then enters blocking receive loop.
     * Call {@link #stop()} from another thread to terminate the loop.
     *
     * @throws IOException if unable to bind to port
     * @throws IllegalStateException if already running
     */
    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Listener is already running");
        }
        
        try {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(config.getHost(), config.getPort()));
            channel.configureBlocking(true);
            
            log.info("UDP telemetry listener started on {}:{}", config.getHost(), config.getPort());
            
            ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
            
            while (running.get()) {
                try {
                    buffer.clear();
                    channel.receive(buffer);
                    buffer.flip();
                    
                    if (buffer.remaining() > 0) {
                        processPacket(buffer);
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        log.error("Error receiving UDP packet: {}", e.getMessage(), e);
                    }
                    // Continue processing despite error
                }
            }
        } finally {
            cleanup();
        }
    }
    
    /**
     * Stops the UDP listener.
     * <p>
     * Signals the receive loop to terminate and closes the channel.
     * Safe to call multiple times.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping UDP telemetry listener...");
            cleanup();
        }
    }
    
    /**
     * Checks if the listener is currently running.
     *
     * @return true if started and not yet stopped
     */
    public boolean isRunning() {
        return running.get();
    }
    
    private void processPacket(ByteBuffer buffer) {
        try {
            PacketHeader header = PacketHeaderDecoder.decode(buffer);
            
            log.trace("Received packet: id={}, session={}, frame={}", 
                      header.getPacketId(), header.getSessionUID(), header.getFrameIdentifier());
            
            // Remaining buffer is packet-specific payload; slice so dispatcher rewind() sees payload from position 0
            ByteBuffer payload = buffer.slice();
            dispatcher.dispatch(header, payload);
            
        } catch (Exception e) {
            log.error("Error processing packet: {}", e.getMessage(), e);
            // Continue receiving despite error
        }
    }
    
    private void cleanup() {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
                log.info("UDP channel closed");
            } catch (IOException e) {
                log.warn("Error closing UDP channel: {}", e.getMessage());
            }
        }
    }
}
