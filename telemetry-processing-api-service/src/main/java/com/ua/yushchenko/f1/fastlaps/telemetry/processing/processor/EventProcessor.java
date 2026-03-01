package com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.DrsDisabledReason;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.RetirementReason;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.SafetyCarEventType;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.SafetyCarType;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionEventMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.SessionEventWriter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket.LiveDataBroadcaster;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Processes Packet Event: logs event, persists to session_events, optional detail as JSON.
 * Lap resolved from payload (e.g. PENA penaltyLapNum) or from session runtime state (e.g. FTLP).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventProcessor {

    private final SessionStateManager stateManager;
    private final SessionEventWriter sessionEventWriter;
    private final SessionEventMapper sessionEventMapper;
    private final LiveDataBroadcaster liveDataBroadcaster;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // Persist to session_events for REST/UI timeline
        Short lap = resolveLap(sessionUid, code, payload);
        Short carIndex = resolveCarIndex(payload);
        String detailJson = buildDetailJson(code, payload);
        SessionEvent saved = sessionEventWriter.write(sessionUid, frameId, lap, code, carIndex, detailJson);
        // Push new event to WebSocket subscribers (Block E optional 19.10)
        SessionEventDto dto = sessionEventMapper.toDto(saved);
        if (dto != null) {
            liveDataBroadcaster.broadcastNewSessionEvent(sessionUid, dto);
        }
    }

    private Short resolveLap(long sessionUid, String code, EventDto payload) {
        if ("PENA".equals(code) && payload.getPenaltyLapNum() != null) {
            return payload.getPenaltyLapNum().shortValue();
        }
        SessionRuntimeState state = stateManager.get(sessionUid);
        if (state == null) {
            return null;
        }
        Integer vehicleIdx = payload.getVehicleIdx() != null ? payload.getVehicleIdx() : payload.getOtherVehicleIdx();
        if (vehicleIdx != null) {
            SessionRuntimeState.CarSnapshot snapshot = state.getSnapshot(vehicleIdx);
            if (snapshot != null && snapshot.getCurrentLap() != null) {
                return snapshot.getCurrentLap().shortValue();
            }
        }
        return null;
    }

    private Short resolveCarIndex(EventDto payload) {
        Integer idx = payload.getVehicleIdx() != null ? payload.getVehicleIdx() : payload.getOtherVehicleIdx();
        return idx != null ? idx.shortValue() : null;
    }

    private String buildDetailJson(String code, EventDto payload) {
        Map<String, Object> map = new HashMap<>();
        switch (code) {
            case "FTLP" -> {
                if (payload.getVehicleIdx() != null) map.put("vehicleIdx", payload.getVehicleIdx());
                if (payload.getLapTime() != null) map.put("lapTime", payload.getLapTime());
            }
            case "PENA" -> {
                if (payload.getVehicleIdx() != null) map.put("vehicleIdx", payload.getVehicleIdx());
                if (payload.getOtherVehicleIdx() != null) map.put("otherVehicleIdx", payload.getOtherVehicleIdx());
                if (payload.getPenaltyType() != null) map.put("penaltyType", payload.getPenaltyType());
                if (payload.getPenaltyLapNum() != null) map.put("penaltyLapNum", payload.getPenaltyLapNum());
                if (payload.getPenaltyTime() != null) map.put("penaltyTime", payload.getPenaltyTime());
                if (payload.getPlacesGained() != null) map.put("placesGained", payload.getPlacesGained());
            }
            case "SCAR" -> {
                if (payload.getSafetyCarType() != null) map.put("safetyCarType", payload.getSafetyCarType());
                if (payload.getSafetyCarEventType() != null) map.put("safetyCarEventType", payload.getSafetyCarEventType());
            }
            case "RTMT" -> {
                if (payload.getVehicleIdx() != null) map.put("vehicleIdx", payload.getVehicleIdx());
                if (payload.getRetirementReason() != null) map.put("retirementReason", payload.getRetirementReason());
            }
            case "DRSD" -> {
                if (payload.getDrsDisabledReason() != null) map.put("drsDisabledReason", payload.getDrsDisabledReason());
            }
            default -> {
                if (payload.getVehicleIdx() != null) map.put("vehicleIdx", payload.getVehicleIdx());
            }
        }
        if (map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event detail: code={}", code, e);
            return null;
        }
    }
}
