package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarStatusRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.CarStatusRawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Writes raw car status samples to car_status_raw.
 * See: implementation_steps_plan.md § 5.7, 6.6.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarStatusRawWriter {

    private final CarStatusRawRepository repository;

    /**
     * Persist one car status sample. Called from CarStatusConsumer when session is ACTIVE.
     */
    public void write(
            Instant ts,
            long sessionUid,
            int frameIdentifier,
            short carIndex,
            CarStatusDto dto,
            float sessionTimeS
    ) {
        if (dto == null) {
            return;
        }
        try {
            Short tractionControl = toShort(dto.getTractionControl());
            Short abs = toShort(dto.getAbs());
            Short fuelMix = toShort(dto.getFuelMix());
            Short tyresCompound = toShort(dto.getTyresCompound());
            Short tyresAgeLaps = toShort(dto.getTyresAgeLaps());

            CarStatusRaw row = CarStatusRaw.builder()
                    .ts(ts)
                    .sessionUid(sessionUid)
                    .frameIdentifier(frameIdentifier)
                    .carIndex(carIndex)
                    .tractionControl(tractionControl)
                    .abs(abs)
                    .fuelInTank(dto.getFuelInTank())
                    .fuelMix(fuelMix)
                    .drsAllowed(dto.getDrsAllowed())
                    .tyresCompound(tyresCompound)
                    .tyresAgeLaps(tyresAgeLaps)
                    .ersStoreEnergy(dto.getErsStoreEnergy())
                    .sessionTimeS(sessionTimeS != 0 ? sessionTimeS : null)
                    .build();
            repository.save(row);
        } catch (Exception e) {
            log.warn("Failed to write car_status_raw: sessionUid={}, frame={}, carIndex={}",
                    sessionUid, frameIdentifier, carIndex, e);
        }
    }

    private static Short toShort(Integer value) {
        if (value == null) return null;
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) return null;
        return value.shortValue();
    }
}
