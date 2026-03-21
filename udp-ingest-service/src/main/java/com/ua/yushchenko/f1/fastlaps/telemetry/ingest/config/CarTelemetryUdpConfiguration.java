package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CarTelemetryUdpProperties.class)
public class CarTelemetryUdpConfiguration {
}
