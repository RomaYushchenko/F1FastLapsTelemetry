package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.config;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.KafkaTelemetryPublisher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.NoOpPublisher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.RetryingPublisher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.ThrottlingPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Configuration for telemetry publisher with decorator chain:
 * throttling → retrying → kafka
 * <p>
 * Manual bean wiring to avoid circular dependencies.
 * If Kafka is disabled, returns a no-op publisher.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TelemetryPublisherProperties.class)
public class TelemetryPublisherConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "f1.telemetry.kafka.enabled", havingValue = "true")
    public TelemetryPublisher kafkaPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            TelemetryPublisherProperties properties) {
        
        log.info("Configuring Kafka telemetry publisher");
        
        // Base implementation
        TelemetryPublisher publisher = new KafkaTelemetryPublisher(kafkaTemplate);
        
        // Add retry decorator
        if (properties.getRetry().isEnabled()) {
            log.info("Enabling retry: maxAttempts={}, initialBackoffMs={}",
                    properties.getRetry().getMaxAttempts(),
                    properties.getRetry().getInitialBackoffMs());
            
            publisher = new RetryingPublisher(
                    publisher,
                    properties.getRetry().getMaxAttempts(),
                    properties.getRetry().getInitialBackoffMs()
            );
        }
        
        // Add throttling decorator
        if (properties.getThrottle().isEnabled()) {
            log.info("Enabling throttling: permitsPerSecond={}",
                    properties.getThrottle().getPermitsPerSecond());
            
            publisher = new ThrottlingPublisher(
                    publisher,
                    properties.getThrottle().getPermitsPerSecond()
            );
        }
        
        log.info("Telemetry publisher configured with {} decorators",
                (properties.getRetry().isEnabled() ? 1 : 0) +
                        (properties.getThrottle().isEnabled() ? 1 : 0));
        
        return publisher;
    }
    
    @Bean
    @ConditionalOnProperty(name = "f1.telemetry.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public TelemetryPublisher noOpPublisher() {
        log.info("Kafka disabled, using NoOpPublisher");
        return new NoOpPublisher();
    }
}
