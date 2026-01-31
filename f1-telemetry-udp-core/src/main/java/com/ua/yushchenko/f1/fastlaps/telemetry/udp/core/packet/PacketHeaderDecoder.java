package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.exception.PacketDecodingException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Decodes F1 UDP packet headers from raw byte buffers.
 * <p>
 * F1 2025 packet header structure (29 bytes):
 * <pre>
 * uint16   packetFormat         // 2025
 * uint8    gameYear             // 25
 * uint8    gameMajorVersion     // e.g., 1
 * uint8    gameMinorVersion     // e.g., 0
 * uint8    packetVersion        // Version of this packet type
 * uint8    packetId             // Identifier for the packet type
 * uint64   sessionUID           // Unique session identifier
 * float    sessionTime          // Session timestamp
 * uint32   frameIdentifier      // Identifier for the frame
 * uint32   overallFrameIdentifier // Overall identifier
 * uint8    playerCarIndex       // Index of player's car
 * uint8    secondaryPlayerCarIndex // Index of secondary player's car (255 if N/A)
 * </pre>
 */
@Slf4j
@UtilityClass
public class PacketHeaderDecoder {
    
    /**
     * Expected header size in bytes.
     */
    public static final int HEADER_SIZE = 29;
    
    /**
     * Decodes packet header from byte buffer.
     * <p>
     * Buffer position is advanced by {@link #HEADER_SIZE} bytes.
     * Remaining bytes after header represent packet-specific payload.
     *
     * @param buffer byte buffer containing packet data (little-endian)
     * @return decoded packet header
     * @throws PacketDecodingException if buffer has insufficient data
     */
    public static PacketHeader decode(ByteBuffer buffer) {
        if (buffer.remaining() < HEADER_SIZE) {
            throw new PacketDecodingException(
                "Insufficient data for packet header: expected " + HEADER_SIZE + " bytes, got " + buffer.remaining()
            );
        }
        
        // Ensure little-endian byte order (F1 uses little-endian)
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        int packetFormat = Short.toUnsignedInt(buffer.getShort());
        short gameYear = (short) Byte.toUnsignedInt(buffer.get());
        short gameMajorVersion = (short) Byte.toUnsignedInt(buffer.get());
        short gameMinorVersion = (short) Byte.toUnsignedInt(buffer.get());
        short packetVersion = (short) Byte.toUnsignedInt(buffer.get());
        short packetId = (short) Byte.toUnsignedInt(buffer.get());
        long sessionUID = buffer.getLong();
        float sessionTime = buffer.getFloat();
        long frameIdentifier = Integer.toUnsignedLong(buffer.getInt());
        long overallFrameIdentifier = Integer.toUnsignedLong(buffer.getInt());
        short playerCarIndex = (short) Byte.toUnsignedInt(buffer.get());
        short secondaryPlayerCarIndex = (short) Byte.toUnsignedInt(buffer.get());
        
        PacketHeader header = PacketHeader.builder()
            .packetFormat(packetFormat)
            .gameYear(gameYear)
            .gameMajorVersion(gameMajorVersion)
            .gameMinorVersion(gameMinorVersion)
            .packetVersion(packetVersion)
            .packetId(packetId)
            .sessionUID(sessionUID)
            .sessionTime(sessionTime)
            .frameIdentifier(frameIdentifier)
            .overallFrameIdentifier(overallFrameIdentifier)
            .playerCarIndex(playerCarIndex)
            .secondaryPlayerCarIndex(secondaryPlayerCarIndex)
            .build();
        
        log.trace("Decoded packet header: packetId={}, sessionUID={}, frameId={}", 
                  packetId, sessionUID, frameIdentifier);
        
        return header;
    }
}
