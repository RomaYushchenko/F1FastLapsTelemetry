package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a handler for a specific F1 UDP packet type.
 * <p>
 * Must be used within a class annotated with {@link F1UdpListener}.
 * <p>
 * Supported method signatures:
 * <ul>
 *   <li>{@code void method(PacketHeader header, ByteBuffer payload)} - Full signature</li>
 *   <li>{@code void method(ByteBuffer payload)} - Payload only</li>
 *   <li>{@code void method(PacketHeader header)} - Header only</li>
 * </ul>
 * <p>
 * Example:
 * <pre>
 * &#64;F1UdpListener
 * public class SessionHandler {
 *     
 *     &#64;F1PacketHandler(packetId = 1)
 *     public void handleSession(PacketHeader header, ByteBuffer payload) {
 *         // Process session packet
 *     }
 *     
 *     &#64;F1PacketHandler(packetId = 2)
 *     public void handleLap(ByteBuffer payload) {
 *         // Process lap data packet
 *     }
 * }
 * </pre>
 *
 * @see F1UdpListener
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface F1PacketHandler {
    
    /**
     * Packet type identifier.
     * <p>
     * Common values:
     * <ul>
     *   <li>1 - Session</li>
     *   <li>2 - Lap Data</li>
     *   <li>6 - Car Telemetry</li>
     *   <li>7 - Car Status</li>
     * </ul>
     *
     * @return packet type ID
     */
    short packetId();
}
