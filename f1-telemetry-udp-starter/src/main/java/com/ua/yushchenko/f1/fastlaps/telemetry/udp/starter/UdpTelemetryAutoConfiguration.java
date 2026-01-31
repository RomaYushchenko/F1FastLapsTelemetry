package com.ua.yushchenko.f1.fastlaps.telemetry.udp.starter;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher.UdpPacketDispatcher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.listener.UdpListenerConfig;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.listener.UdpTelemetryListener;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.config.UdpDispatcherConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.io.IOException;

/**
 * Spring Boot auto-configuration for UDP telemetry listener.
 * <p>
 * Automatically creates and manages the UDP listener lifecycle when enabled.
 * Imports the dispatcher configuration and enables component scanning for 
 * packet handler registry and handlers.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(UdpTelemetryProperties.class)
@ConditionalOnProperty(prefix = "f1.telemetry.udp", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(UdpDispatcherConfiguration.class)
@ComponentScan(basePackages = "com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring")
public class UdpTelemetryAutoConfiguration {

    private UdpTelemetryListener listener;

    /**
     * Creates the UDP telemetry listener bean.
     *
     * @param properties  UDP configuration properties
     * @param dispatcher  packet dispatcher (registered handlers)
     * @return configured UDP listener
     */
    @Bean
    public UdpTelemetryListener udpTelemetryListener(
            UdpTelemetryProperties properties,
            UdpPacketDispatcher dispatcher) {
        
        log.info("Creating UDP telemetry listener: host={}, port={}", 
                properties.getHost(), properties.getPort());
        
        UdpListenerConfig config = UdpListenerConfig.builder()
                .host(properties.getHost())
                .port(properties.getPort())
                .build();
        
        this.listener = new UdpTelemetryListener(config, dispatcher);
        
        return listener;
    }

    /**
     * Starts the UDP listener after Spring context is fully initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startListener() {
        if (listener != null && !listener.isRunning()) {
            log.info("Starting UDP telemetry listener");
            try {
                listener.start();
                log.info("UDP telemetry listener started successfully");
            } catch (IOException e) {
                log.error("Failed to start UDP telemetry listener", e);
                throw new RuntimeException("Failed to start UDP listener", e);
            }
        }
    }

    /**
     * Stops the UDP listener gracefully when Spring context is shutting down.
     */
    @EventListener(ContextClosedEvent.class)
    public void stopListener() {
        if (listener != null && listener.isRunning()) {
            log.info("Stopping UDP telemetry listener");
            listener.stop();
            log.info("UDP telemetry listener stopped");
        }
    }
}
