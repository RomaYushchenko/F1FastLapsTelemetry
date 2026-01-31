package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet;

import lombok.Builder;
import lombok.Value;

/**
 * F1 UDP telemetry packet header (F1 2025 format).
 * <p>
 * Contains metadata common to all packet types:
 * <ul>
 *   <li>Game version and packet format</li>
 *   <li>Session and timing identifiers</li>
 *   <li>Frame counters for ordering</li>
 *   <li>Player car index</li>
 * </ul>
 * <p>
 * Header is 29 bytes at the start of every UDP packet.
 */
@Value
@Builder
public class PacketHeader {
    
    /**
     * Packet format version (2025 for F1 25).
     */
    int packetFormat;
    
    /**
     * Game year (last two digits, e.g., 25 for 2025).
     */
    short gameYear;
    
    /**
     * Game major version.
     */
    short gameMajorVersion;
    
    /**
     * Game minor version.
     */
    short gameMinorVersion;
    
    /**
     * Packet version (increases with each packet format change).
     */
    short packetVersion;
    
    /**
     * Packet type identifier (1=Session, 2=Lap, 6=Telemetry, 7=Status, etc.).
     */
    short packetId;
    
    /**
     * Unique session identifier.
     */
    long sessionUID;
    
    /**
     * Session timestamp in seconds.
     */
    float sessionTime;
    
    /**
     * Frame identifier (increments each frame).
     */
    long frameIdentifier;
    
    /**
     * Overall frame identifier (includes flashbacks/rewinds).
     */
    long overallFrameIdentifier;
    
    /**
     * Index of the player's car in the array (0-21).
     */
    short playerCarIndex;
    
    /**
     * Secondary player car index (split-screen, 255 if not used).
     */
    short secondaryPlayerCarIndex;
}
