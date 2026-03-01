package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps SessionEvent entity to REST SessionEventDto.
 * Detail JSON string is parsed to Object for API; on parse failure detail is left null.
 * Block E — Session events.
 */
@Slf4j
@Component
public class SessionEventMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public SessionEventDto toDto(SessionEvent entity) {
        if (entity == null) {
            return null;
        }
        Object detail = parseDetail(entity.getDetail());
        return SessionEventDto.builder()
                .lap(entity.getLap() != null ? entity.getLap().intValue() : null)
                .eventCode(entity.getEventCode())
                .carIndex(entity.getCarIndex() != null ? entity.getCarIndex().intValue() : null)
                .detail(detail)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private static Object parseDetail(String detailJson) {
        if (detailJson == null || detailJson.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(detailJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.debug("Could not parse event detail JSON: {}", detailJson);
            return null;
        }
    }
}
