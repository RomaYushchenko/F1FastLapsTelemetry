package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides {@link ObjectMapper} bean for udp-ingest-service.
 * This module uses spring-boot-starter (no web), so Jackson is not auto-configured;
 * the aspects (InboundUdpLoggingAspect, OutboundEventLoggingAspect) need ObjectMapper for JSON logging.
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
