package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.config;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.NoOpPublisher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.RetryingPublisher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.ThrottlingPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TelemetryPublisherConfigurationTest {
    
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TelemetryPublisherConfiguration.class)
            .withBean(KafkaTemplate.class, () -> mockKafkaTemplate());
    
    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, Object> mockKafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
    
    @Test
    void shouldCreateNoOpPublisherWhenKafkaDisabled() {
        contextRunner
                .withPropertyValues("f1.telemetry.kafka.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(TelemetryPublisher.class);
                    TelemetryPublisher publisher = context.getBean(TelemetryPublisher.class);
                    assertThat(publisher).isInstanceOf(NoOpPublisher.class);
                });
    }
    
    @Test
    void shouldCreateNoOpPublisherWhenPropertyMissing() {
        // When enabled property is missing, should default to NoOp
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(TelemetryPublisher.class);
                    TelemetryPublisher publisher = context.getBean(TelemetryPublisher.class);
                    assertThat(publisher).isInstanceOf(NoOpPublisher.class);
                });
    }
    
    @Test
    void shouldCreateKafkaPublisherWhenEnabled() {
        contextRunner
                .withPropertyValues("f1.telemetry.kafka.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(TelemetryPublisher.class);
                    TelemetryPublisher publisher = context.getBean(TelemetryPublisher.class);
                    
                    // Should be wrapped in decorators (retry enabled by default)
                    assertThat(publisher).isInstanceOf(RetryingPublisher.class);
                });
    }
    
    @Test
    void shouldApplyThrottlingWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "f1.telemetry.kafka.enabled=true",
                        "f1.telemetry.kafka.throttle.enabled=true",
                        "f1.telemetry.kafka.throttle.permits-per-second=50"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(TelemetryPublisher.class);
                    TelemetryPublisher publisher = context.getBean(TelemetryPublisher.class);
                    
                    // Outermost decorator should be throttling
                    assertThat(publisher).isInstanceOf(ThrottlingPublisher.class);
                });
    }
    
    @Test
    void shouldNotApplyRetryWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "f1.telemetry.kafka.enabled=true",
                        "f1.telemetry.kafka.retry.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(TelemetryPublisher.class);
                    TelemetryPublisher publisher = context.getBean(TelemetryPublisher.class);
                    
                    // Should NOT be RetryingPublisher
                    assertThat(publisher).isNotInstanceOf(RetryingPublisher.class);
                });
    }
    
    @Test
    void shouldApplyBothDecorators() {
        contextRunner
                .withPropertyValues(
                        "f1.telemetry.kafka.enabled=true",
                        "f1.telemetry.kafka.retry.enabled=true",
                        "f1.telemetry.kafka.retry.max-attempts=5",
                        "f1.telemetry.kafka.throttle.enabled=true",
                        "f1.telemetry.kafka.throttle.permits-per-second=100"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(TelemetryPublisher.class);
                    TelemetryPublisher publisher = context.getBean(TelemetryPublisher.class);
                    
                    // Chain: throttling → retrying → kafka
                    assertThat(publisher).isInstanceOf(ThrottlingPublisher.class);
                });
    }
}
