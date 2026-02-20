package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TyreWearSnapshot;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TyreWearState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.carDamage topic.
 * Updates in-memory tyre wear state per session+car; TyreWearRecorder persists it when a lap is finalized.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarDamageConsumer {

    private final SessionLifecycleService lifecycleService;
    private final TyreWearState tyreWearState;

    @KafkaListener(
            topics = "telemetry.carDamage",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(CarDamageEvent event, Acknowledgment acknowledgment) {
        try {
            long sessionUid = event.getSessionUID();
            int carIndex = event.getCarIndex();

            lifecycleService.ensureSessionActive(sessionUid);

            if (!lifecycleService.shouldProcessPacket(sessionUid)) {
                acknowledgment.acknowledge();
                return;
            }

            CarDamageDto dto = event.getPayload();
            if (dto == null) {
                acknowledgment.acknowledge();
                return;
            }

            TyreWearSnapshot snapshot = new TyreWearSnapshot(
                    dto.getTyresWearFL(),
                    dto.getTyresWearFR(),
                    dto.getTyresWearRL(),
                    dto.getTyresWearRR()
            );
            tyreWearState.update(sessionUid, carIndex, snapshot);

            log.debug("Updated tyre wear state: sessionUid={}, carIndex={}", sessionUid, carIndex);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing car damage", e);
            throw e;
        }
    }
}
