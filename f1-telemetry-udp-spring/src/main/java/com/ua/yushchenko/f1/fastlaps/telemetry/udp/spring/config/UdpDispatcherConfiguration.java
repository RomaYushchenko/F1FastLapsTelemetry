package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.config;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher.SimpleUdpPacketDispatcher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher.UdpPacketConsumer;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher.UdpPacketDispatcher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.registry.PacketHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that wires the packet handler registry to the dispatcher.
 * <p>
 * Creates a {@link UdpPacketDispatcher} bean and registers all consumers
 * from the {@link PacketHandlerRegistry}.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UdpDispatcherConfiguration {
    
    private final PacketHandlerRegistry registry;
    
    /**
     * Creates and configures the UDP packet dispatcher.
     * <p>
     * All consumers from the registry are registered with the dispatcher.
     *
     * @return configured dispatcher
     */
    @Bean
    public UdpPacketDispatcher udpPacketDispatcher() {
        log.info("Creating UDP packet dispatcher...");
        
        SimpleUdpPacketDispatcher dispatcher = new SimpleUdpPacketDispatcher();
        
        // Register all consumers from registry
        for (UdpPacketConsumer consumer : registry.getConsumers()) {
            dispatcher.registerConsumer(consumer);
        }
        
        log.info("UDP packet dispatcher created with {} consumer(s)", registry.getConsumerCount());
        
        return dispatcher;
    }
}
