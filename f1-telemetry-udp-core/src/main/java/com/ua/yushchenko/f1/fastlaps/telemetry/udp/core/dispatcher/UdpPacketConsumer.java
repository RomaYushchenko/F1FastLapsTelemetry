package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.nio.ByteBuffer;

/**
 * Consumer that handles UDP packets of a specific type.
 * <p>
 * Implementations process the packet payload after header has been decoded.
 * The payload ByteBuffer is positioned at the start of packet-specific data
 * (i.e., after the 29-byte header).
 */
public interface UdpPacketConsumer {
    
    /**
     * Returns the packet type this consumer handles.
     * <p>
     * Examples: 1 (Session), 2 (Lap Data), 6 (Telemetry), 7 (Car Status).
     *
     * @return packet type identifier
     */
    short packetId();
    
    /**
     * Handles a UDP packet.
     * <p>
     * Called by dispatcher when a packet matching {@link #packetId()} is received.
     * <p>
     * The payload buffer is positioned at the start of packet-specific data.
     * Consumer is free to modify buffer position but should not modify the buffer content.
     *
     * @param header decoded packet header
     * @param payload remaining bytes after header (packet-specific data)
     */
    void handle(PacketHeader header, ByteBuffer payload);
}
