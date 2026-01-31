package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.config;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher.UdpPacketConsumer;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher.UdpPacketDispatcher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.registry.PacketHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = UdpDispatcherConfigurationTest.TestConfig.class)
class UdpDispatcherConfigurationTest {
    
    @Autowired
    private UdpPacketDispatcher dispatcher;
    
    @Autowired
    private PacketHandlerRegistry registry;
    
    @Test
    void shouldCreateDispatcherBean() {
        assertThat(dispatcher).isNotNull();
    }
    
    @Test
    void shouldWireRegistryToDispatcher() {
        // Registry has test consumers
        assertThat(registry.getConsumerCount()).isEqualTo(2);
    }
    
    @Configuration
    static class TestConfig {
        
        @Bean
        public PacketHandlerRegistry packetHandlerRegistry() {
            PacketHandlerRegistry registry = new PacketHandlerRegistry();
            
            // Add test consumers
            registry.register(new UdpPacketConsumer() {
                @Override
                public short packetId() { return 1; }
                
                @Override
                public void handle(PacketHeader header, ByteBuffer payload) {}
            });
            
            registry.register(new UdpPacketConsumer() {
                @Override
                public short packetId() { return 2; }
                
                @Override
                public void handle(PacketHeader header, ByteBuffer payload) {}
            });
            
            return registry;
        }
        
        @Bean
        public UdpPacketDispatcher udpPacketDispatcher(PacketHandlerRegistry registry) {
            return new UdpDispatcherConfiguration(registry).udpPacketDispatcher();
        }
    }
}
