package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1PacketHandler;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1UdpListener;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.registry.PacketHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicatePacketIdValidationTest {
    
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();
    
    @Test
    void shouldThrowExceptionForDuplicatePacketId() {
        contextRunner
            .withUserConfiguration(DuplicateHandlerConfig.class)
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasMessageContaining("Duplicate @F1PacketHandler for packetId=1");
            });
    }
    
    @Configuration
    static class DuplicateHandlerConfig {
        
        @Bean
        public PacketHandlerRegistry packetHandlerRegistry() {
            return new PacketHandlerRegistry();
        }
        
        @Bean
        public F1PacketHandlerPostProcessor postProcessor(PacketHandlerRegistry registry) {
            return new F1PacketHandlerPostProcessor(registry);
        }
        
        @Bean
        public DuplicateListener duplicateListener() {
            return new DuplicateListener();
        }
    }
    
    @F1UdpListener
    static class DuplicateListener {
        
        @F1PacketHandler(packetId = 1)
        public void handleFirst(PacketHeader header, ByteBuffer payload) {
        }
        
        @F1PacketHandler(packetId = 1)  // Duplicate!
        public void handleSecond(PacketHeader header, ByteBuffer payload) {
        }
    }
}
