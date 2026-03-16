package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.metrics;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketHealthEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Publishes current packet loss ratio per active session to Kafka so that
 * telemetry-processing-api-service can expose it via the diagnostics API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PacketHealthPublisher {

    public static final String TOPIC = "telemetry.packetHealth";

    private final PacketLossMetricsRecorder packetLossMetricsRecorder;
    private final TelemetryPublisher publisher;

    @Scheduled(fixedDelayString = "${f1.telemetry.packet-health.publish-interval-ms:5000}")
    public void publishPacketHealth() {
        Set<Long> sessionUids = packetLossMetricsRecorder.getActiveSessionUids();
        if (sessionUids.isEmpty()) {
            return;
        }
        long nowMillis = System.currentTimeMillis();
        for (Long sessionUid : sessionUids) {
            Optional<Double> ratioOpt = packetLossMetricsRecorder.getPacketLossRatio(sessionUid);
            if (ratioOpt.isEmpty()) {
                continue;
            }
            double ratio = ratioOpt.get();
            PacketHealthEvent event = new PacketHealthEvent(sessionUid, ratio, nowMillis);
            String key = String.valueOf(sessionUid);
            try {
                publisher.publish(TOPIC, key, event);
                log.trace("Published packet health: sessionUid={}, ratio={}", sessionUid, ratio);
            } catch (Exception e) {
                log.warn("Failed to publish packet health for sessionUid={}", sessionUid, e);
            }
        }
    }
}
