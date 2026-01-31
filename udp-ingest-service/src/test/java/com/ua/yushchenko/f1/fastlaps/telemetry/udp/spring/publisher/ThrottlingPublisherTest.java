package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ThrottlingPublisherTest {
    
    @Test
    void shouldThrottlePublishRate() {
        // Given
        TelemetryPublisher delegate = mock(TelemetryPublisher.class);
        ThrottlingPublisher publisher = new ThrottlingPublisher(delegate, 10.0); // 10 permits/sec
        
        long startTime = System.currentTimeMillis();
        
        // When - publish 5 messages rapidly
        for (int i = 0; i < 5; i++) {
            publisher.publish("topic", "key-" + i, "value-" + i);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Then
        verify(delegate, times(5)).publish(anyString(), anyString(), any());
        
        // At 10 permits/sec, 5 messages should take at least 400ms
        // (0ms + 100ms + 100ms + 100ms + 100ms)
        assert duration >= 350 : "Expected throttling delays, got " + duration + "ms";
    }
    
    @Test
    void shouldNotThrottleWhenBelowLimit() {
        // Given
        TelemetryPublisher delegate = mock(TelemetryPublisher.class);
        ThrottlingPublisher publisher = new ThrottlingPublisher(delegate, 1000.0); // High limit
        
        long startTime = System.currentTimeMillis();
        
        // When - publish 3 messages
        for (int i = 0; i < 3; i++) {
            publisher.publish("topic", "key-" + i, "value-" + i);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Then
        verify(delegate, times(3)).publish(anyString(), anyString(), any());
        
        // Should be very fast (no throttling)
        assert duration < 100 : "Expected no throttling, got " + duration + "ms";
    }
}
