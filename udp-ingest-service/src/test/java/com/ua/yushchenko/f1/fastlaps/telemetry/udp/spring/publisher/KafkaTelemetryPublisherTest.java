package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaTelemetryPublisherTest {
    
    @Test
    void shouldPublishMessageToKafka() {
        // Given
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaTelemetryPublisher publisher = new KafkaTelemetryPublisher(kafkaTemplate);
        
        String topic = "test.topic";
        String key = "test-key";
        Object value = new TestMessage("data");
        
        // When
        publisher.publish(topic, key, value);
        
        // Then
        verify(kafkaTemplate).send(eq(topic), eq(key), eq(value));
    }
    
    private record TestMessage(String data) {}
}
