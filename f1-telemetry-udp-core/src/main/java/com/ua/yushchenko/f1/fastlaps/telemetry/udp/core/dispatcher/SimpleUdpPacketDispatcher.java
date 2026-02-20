package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple implementation of UDP packet dispatcher.
 * <p>
 * Maintains a registry of consumers grouped by packet type.
 * Thread-safe for registration and dispatch operations.
 */
@Slf4j
public class SimpleUdpPacketDispatcher implements UdpPacketDispatcher {
    
    private final Map<Short, List<UdpPacketConsumer>> consumers = new HashMap<>();
    private final Object lock = new Object();
    
    @Override
    public void dispatch(PacketHeader header, ByteBuffer payload) {
        List<UdpPacketConsumer> handlers;
        
        synchronized (lock) {
            handlers = consumers.get(header.getPacketId());
            if (handlers == null || handlers.isEmpty()) {
                log.trace("No consumers registered for packetId={}", header.getPacketId());
                return;
            }
            // Create defensive copy to avoid concurrent modification during iteration
            handlers = new ArrayList<>(handlers);
        }
        
        log.debug("Dispatching packetId={} to {} consumer(s)", header.getPacketId(), handlers.size());
        
        for (UdpPacketConsumer consumer : handlers) {
            try {
                // Contract: payload is packet data only (no header). Rewind so each consumer sees from start.
                payload.rewind();
                consumer.handle(header, payload);
            } catch (Exception e) {
                log.error("Error in consumer {} handling packetId={}: {}", 
                          consumer.getClass().getSimpleName(), header.getPacketId(), e.getMessage(), e);
                // Continue dispatching to other consumers despite error
            }
        }
    }
    
    @Override
    public void registerConsumer(UdpPacketConsumer consumer) {
        synchronized (lock) {
            consumers.computeIfAbsent(consumer.packetId(), k -> new ArrayList<>()).add(consumer);
        }
        log.info("Registered consumer {} for packetId={}", 
                 consumer.getClass().getSimpleName(), consumer.packetId());
    }
}
