package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.sessionEvent;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionEventMapper")
class SessionEventMapperTest {

    @InjectMocks
    private SessionEventMapper mapper;

    @Test
    @DisplayName("toDto повертає null коли entity null")
    void toDto_returnsNull_whenEntityNull() {
        SessionEventDto result = mapper.toDto(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDto мапить entity у DTO з detail як об'єкт")
    void toDto_mapsEntityToDto_withDetailAsObject() {
        SessionEvent entity = sessionEvent();

        SessionEventDto result = mapper.toDto(entity);

        assertThat(result).isNotNull();
        assertThat(result.getLap()).isEqualTo(24);
        assertThat(result.getEventCode()).isEqualTo("FTLP");
        assertThat(result.getCarIndex()).isEqualTo(0);
        assertThat(result.getDetail()).isInstanceOf(java.util.Map.class);
        assertThat(result.getCreatedAt()).isEqualTo(entity.getCreatedAt());
    }
}
