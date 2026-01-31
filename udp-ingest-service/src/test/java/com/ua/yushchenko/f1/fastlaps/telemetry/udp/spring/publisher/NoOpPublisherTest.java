package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpPublisherTest {
    
    @Test
    void shouldDoNothingOnPublish() {
        // Given
        NoOpPublisher publisher = new NoOpPublisher();
        
        // When/Then - should not throw exception
        publisher.publish("topic", "key", "value");
        
        // Verify it completes without error
        assertThat(publisher).isNotNull();
    }
}
