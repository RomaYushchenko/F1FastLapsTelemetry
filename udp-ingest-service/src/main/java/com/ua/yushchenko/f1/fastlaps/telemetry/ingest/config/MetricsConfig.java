package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides Clock for metrics (e.g. PacketLossMetricsRecorder rolling window).
 * Uses system default zone so production and tests can override with a fixed clock if needed.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
