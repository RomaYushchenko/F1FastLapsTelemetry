package com.ua.yushchenko.f1.fastlaps.telemetry.udp.starter;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.listener.UdpTelemetryListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class UdpTelemetryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UdpTelemetryAutoConfiguration.class));

    @Test
    void shouldCreateListenerWhenEnabled() {
        this.contextRunner
                .withPropertyValues(
                        "f1.telemetry.udp.enabled=true",
                        "f1.telemetry.udp.port=20777"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(UdpTelemetryListener.class);
                    assertThat(context).hasSingleBean(UdpTelemetryProperties.class);
                    
                    UdpTelemetryProperties properties = context.getBean(UdpTelemetryProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getPort()).isEqualTo(20777);
                    assertThat(properties.getHost()).isEqualTo("0.0.0.0");
                });
    }

    @Test
    void shouldNotCreateListenerWhenDisabled() {
        this.contextRunner
                .withPropertyValues("f1.telemetry.udp.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(UdpTelemetryListener.class);
                });
    }

    @Test
    void shouldUseDefaultProperties() {
        this.contextRunner
                .run(context -> {
                    UdpTelemetryProperties properties = context.getBean(UdpTelemetryProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getPort()).isEqualTo(20777);
                    assertThat(properties.getHost()).isEqualTo("0.0.0.0");
                    assertThat(properties.getBufferSize()).isEqualTo(2048);
                });
    }

    @Test
    void shouldRespectCustomProperties() {
        this.contextRunner
                .withPropertyValues(
                        "f1.telemetry.udp.host=127.0.0.1",
                        "f1.telemetry.udp.port=30777",
                        "f1.telemetry.udp.buffer-size=4096"
                )
                .run(context -> {
                    UdpTelemetryProperties properties = context.getBean(UdpTelemetryProperties.class);
                    assertThat(properties.getHost()).isEqualTo("127.0.0.1");
                    assertThat(properties.getPort()).isEqualTo(30777);
                    assertThat(properties.getBufferSize()).isEqualTo(4096);
                });
    }
}
