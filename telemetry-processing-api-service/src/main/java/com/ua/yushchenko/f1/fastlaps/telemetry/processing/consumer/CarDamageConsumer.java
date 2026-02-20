package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor.CarDamageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.carDamage topic. Thin: deserialize, ensureSession, shouldProcess, then CarDamageProcessor.
 * See: implementation_phases.md Phase 5.1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarDamageConsumer {

    private final SessionLifecycleService lifecycleService;
    private final CarDamageProcessor carDamageProcessor;

    @KafkaListener(
            topics = "telemetry.carDamage",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(CarDamageEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            log.warn("Skipping record: deserialization failed (e.g. old format without @type)");
            acknowledgment.acknowledge();
            return;
        }
        try {
            long sessionUid = event.getSessionUID();
            short carIndex = (short) event.getCarIndex();

            lifecycleService.ensureSessionActive(sessionUid);
            if (!lifecycleService.shouldProcessPacket(sessionUid)) {
                acknowledgment.acknowledge();
                return;
            }

            carDamageProcessor.process(sessionUid, carIndex, event.getPayload());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing car damage", e);
            throw e;
        }
    }
}
