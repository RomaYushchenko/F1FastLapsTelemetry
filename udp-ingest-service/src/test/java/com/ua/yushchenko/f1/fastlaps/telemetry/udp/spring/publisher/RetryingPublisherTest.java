package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RetryingPublisherTest {
    
    @Test
    void shouldSucceedOnFirstAttempt() {
        // Given
        TelemetryPublisher delegate = mock(TelemetryPublisher.class);
        RetryingPublisher publisher = new RetryingPublisher(delegate, 3, 10);
        
        // When
        publisher.publish("topic", "key", "value");
        
        // Then
        verify(delegate, times(1)).publish("topic", "key", "value");
    }
    
    @Test
    void shouldRetryOnFailure() {
        // Given
        TelemetryPublisher delegate = mock(TelemetryPublisher.class);
        doThrow(new RuntimeException("Temporary failure"))
                .doNothing()
                .when(delegate).publish(anyString(), anyString(), any());
        
        RetryingPublisher publisher = new RetryingPublisher(delegate, 3, 10);
        
        // When
        publisher.publish("topic", "key", "value");
        
        // Then
        verify(delegate, times(2)).publish("topic", "key", "value");
    }
    
    @Test
    void shouldThrowAfterMaxAttempts() {
        // Given
        TelemetryPublisher delegate = mock(TelemetryPublisher.class);
        doThrow(new RuntimeException("Persistent failure"))
                .when(delegate).publish(anyString(), anyString(), any());
        
        RetryingPublisher publisher = new RetryingPublisher(delegate, 3, 10);
        
        // When/Then
        assertThatThrownBy(() -> publisher.publish("topic", "key", "value"))
                .isInstanceOf(PublishException.class)
                .hasMessageContaining("Failed after 3 attempts");
        
        verify(delegate, times(3)).publish("topic", "key", "value");
    }
    
    @Test
    void shouldUseExponentialBackoff() {
        // Given
        TelemetryPublisher delegate = mock(TelemetryPublisher.class);
        doThrow(new RuntimeException("Failure"))
                .doThrow(new RuntimeException("Failure"))
                .doNothing()
                .when(delegate).publish(anyString(), anyString(), any());
        
        RetryingPublisher publisher = new RetryingPublisher(delegate, 5, 50);
        
        long startTime = System.currentTimeMillis();
        
        // When
        publisher.publish("topic", "key", "value");
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Then
        // Should wait: 50ms (1st retry) + 100ms (2nd retry) = 150ms minimum
        verify(delegate, times(3)).publish("topic", "key", "value");
        // Allow some margin for test execution time
        assert duration >= 140 : "Expected backoff delays, got " + duration + "ms";
    }
}
