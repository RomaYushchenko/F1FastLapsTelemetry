package com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.DrsDisabledReason;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.RetirementReason;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.SafetyCarEventType;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.SafetyCarType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Processes Packet Event: logs event and optional detail; can extend to update session runtime state
 * (e.g. DRS disabled reason for Live) or persist to session_events table.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventProcessor {

    public void process(long sessionUid, int frameId, EventDto payload) {
        if (payload == null) {
            log.warn("Event payload is null: sessionUid={}, frameId={}", sessionUid, frameId);
            return;
        }
        log.debug("process: sessionUid={}, frameId={}, code={}", sessionUid, frameId, payload.getEventCode());

        String code = payload.getEventCode();
        if (code == null) {
            return;
        }

        switch (code) {
            case "DRSD" -> {
                Integer reason = payload.getDrsDisabledReason();
                if (reason != null) {
                    DrsDisabledReason r = DrsDisabledReason.fromCode(reason);
                    log.info("DRS disabled: sessionUid={}, reason={} ({})", sessionUid, r.getDisplayName(), reason);
                }
            }
            case "SCAR" -> {
                Integer scType = payload.getSafetyCarType();
                Integer evType = payload.getSafetyCarEventType();
                if (scType != null || evType != null) {
                    SafetyCarType t = SafetyCarType.fromCode(scType != null ? scType : -1);
                    SafetyCarEventType e = SafetyCarEventType.fromCode(evType != null ? evType : -1);
                    log.info("Safety car event: sessionUid={}, type={}, event={}", sessionUid, t.getDisplayName(), e.getDisplayName());
                }
            }
            case "RTMT" -> {
                Integer reason = payload.getRetirementReason();
                Integer vehicleIdx = payload.getVehicleIdx();
                if (reason != null || vehicleIdx != null) {
                    log.info("Retirement: sessionUid={}, vehicleIdx={}, reason={}", sessionUid, vehicleIdx,
                            reason != null ? RetirementReason.fromCode(reason).getDisplayName() : null);
                }
            }
            default -> log.debug("Event: sessionUid={}, code={}", sessionUid, code);
        }
    }
}
