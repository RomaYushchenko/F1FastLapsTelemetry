package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1PacketHandler;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1UdpListener;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.registry.PacketHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = F1PacketHandlerPostProcessorTest.TestConfig.class)
class F1PacketHandlerPostProcessorTest {
    
    @Autowired
    private PacketHandlerRegistry registry;
    
    @Test
    void shouldRegisterHandlerMethods() {
        // Assert: Handlers from ValidListener should be registered
        assertThat(registry.getConsumerCount()).isEqualTo(3);
    }
    
    @Test
    void shouldNotRegisterBeanWithoutF1UdpListener() {
        // The NonListenerBean should not be processed
        // All 3 handlers come from ValidListener only
        assertThat(registry.getConsumerCount()).isEqualTo(3);
    }
    
    @Configuration
    static class TestConfig {
        
        @Bean
        public PacketHandlerRegistry packetHandlerRegistry() {
            return new PacketHandlerRegistry();
        }
        
        @Bean
        public F1PacketHandlerPostProcessor postProcessor(PacketHandlerRegistry registry) {
            return new F1PacketHandlerPostProcessor(registry);
        }
        
        @Bean
        public ValidListener validListener() {
            return new ValidListener();
        }
        
        @Bean
        public NonListenerBean nonListenerBean() {
            return new NonListenerBean();
        }
    }
    
    @F1UdpListener
    static class ValidListener {
        
        @F1PacketHandler(packetId = 1)
        public void handleSession(PacketHeader header, ByteBuffer payload) {
        }
        
        @F1PacketHandler(packetId = 2)
        public void handleLap(ByteBuffer payload) {
        }
        
        @F1PacketHandler(packetId = 6)
        public void handleTelemetry(PacketHeader header) {
        }
    }
    
    static class NonListenerBean {
        
        @F1PacketHandler(packetId = 99)
        public void shouldNotBeRegistered(PacketHeader header, ByteBuffer payload) {
        }
    }
}
