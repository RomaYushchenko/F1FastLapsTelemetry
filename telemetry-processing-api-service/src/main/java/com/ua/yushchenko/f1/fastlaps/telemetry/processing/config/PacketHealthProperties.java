package com.ua.yushchenko.f1.fastlaps.telemetry.processing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for packet health thresholds.
 */
@Data
@Component
@ConfigurationProperties(prefix = "packet-health")
public class PacketHealthProperties {

    /**
     * Minimum percent for GOOD band (inclusive).
     * Default: 95.
     * Binds from property: packet-health.threshold.good-min-percent
     */
    private int thresholdGoodMinPercent = 95;

    /**
     * Minimum percent for OK band (inclusive).
     * Default: 80.
     * Binds from property: packet-health.threshold.ok-min-percent
     */
    private int thresholdOkMinPercent = 80;

    public int getGoodMinPercent() {
        return thresholdGoodMinPercent;
    }

    public int getOkMinPercent() {
        return thresholdOkMinPercent;
    }
}

