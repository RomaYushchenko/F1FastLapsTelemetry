package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a UDP packet listener.
 * <p>
 * Classes annotated with {@code @F1UdpListener} are automatically registered as Spring beans
 * and their methods annotated with {@link F1PacketHandler} are scanned and registered
 * as packet consumers.
 * <p>
 * Example:
 * <pre>
 * &#64;F1UdpListener
 * public class TelemetryHandler {
 *     
 *     &#64;F1PacketHandler(packetId = 6)
 *     public void handleTelemetry(PacketHeader header, ByteBuffer payload) {
 *         // Process car telemetry packet
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface F1UdpListener {
}
