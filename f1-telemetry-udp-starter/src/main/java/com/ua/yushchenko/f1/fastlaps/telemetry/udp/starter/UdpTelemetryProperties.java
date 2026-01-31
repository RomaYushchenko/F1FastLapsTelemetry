package com.ua.yushchenko.f1.fastlaps.telemetry.udp.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for UDP telemetry listener.
 */
@Data
@ConfigurationProperties(prefix = "f1.telemetry.udp")
public class UdpTelemetryProperties {

    /**
     * Enable UDP telemetry listener.
     */
    private boolean enabled = true;

    /**
     * UDP host to bind to.
     */
    private String host = "0.0.0.0";

    /**
     * UDP port to listen on.
     */
    private int port = 20777;

    /**
     * Buffer size for UDP packets.
     */
    private int bufferSize = 2048;
}
