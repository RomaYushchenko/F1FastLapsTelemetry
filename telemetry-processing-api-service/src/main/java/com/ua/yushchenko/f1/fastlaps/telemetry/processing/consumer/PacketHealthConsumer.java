package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketHealthEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics.PacketLossRatioStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.packetHealth topic. Updates in-memory store
 * so DiagnosticsService can return packet health via REST API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PacketHealthConsumer {

    private final PacketLossRatioStore packetLossRatioStore;

    @KafkaListener(
            topics = "telemetry.packetHealth",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "packetHealthContainerFactory"
    )
    public void consume(PacketHealthEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            log.warn("Skipping packet health record: deserialization failed");
            acknowledgment.acknowledge();
            return;
        }
        try {
            long sessionUid = event.getSessionUid();
            double ratio = Math.max(0.0d, Math.min(1.0d, event.getPacketLossRatio()));
            packetLossRatioStore.set(sessionUid, ratio);
            log.trace("Updated packet health: sessionUid={}, ratio={}", sessionUid, ratio);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing packet health event", e);
            throw e;
        }
    }
}
