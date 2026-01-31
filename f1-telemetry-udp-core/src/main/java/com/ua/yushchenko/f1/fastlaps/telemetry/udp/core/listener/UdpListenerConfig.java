package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.listener;

import lombok.Builder;
import lombok.Value;

/**
 * Configuration for UDP telemetry listener.
 */
@Value
@Builder
public class UdpListenerConfig {
    
    /**
     * Host/interface to bind to.
     * Use "0.0.0.0" to listen on all interfaces.
     */
    @Builder.Default
    String host = "0.0.0.0";
    
    /**
     * UDP port to listen on.
     * F1 games typically use port 20777.
     */
    @Builder.Default
    int port = 20777;
}
