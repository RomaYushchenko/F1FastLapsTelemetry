package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.nio.ByteBuffer;

/**
 * Dispatches decoded UDP packets to registered consumers.
 * <p>
 * Routes packets based on packet type (packetId) to all registered
 * consumers that handle that packet type.
 */
public interface UdpPacketDispatcher {
    
    /**
     * Dispatches a packet to all registered consumers matching the packet type.
     * <p>
     * If no consumers are registered for the packet type, the packet is silently ignored.
     * Exceptions thrown by consumers are caught and logged, but do not stop
     * dispatching to other consumers.
     *
     * @param header decoded packet header
     * @param payload remaining bytes after header (packet-specific data)
     */
    void dispatch(PacketHeader header, ByteBuffer payload);
    
    /**
     * Registers a consumer for a specific packet type.
     * <p>
     * Multiple consumers can be registered for the same packet type.
     * Consumers are invoked in registration order.
     *
     * @param consumer packet consumer to register
     */
    void registerConsumer(UdpPacketConsumer consumer);
}
