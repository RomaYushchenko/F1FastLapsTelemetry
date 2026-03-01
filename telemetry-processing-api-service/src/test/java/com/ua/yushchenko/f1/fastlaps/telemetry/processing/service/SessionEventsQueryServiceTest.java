package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionEventMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionEventsQueryService")
class SessionEventsQueryServiceTest {

    @Mock
    private SessionEventRepository sessionEventRepository;
    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private SessionEventMapper sessionEventMapper;

    @InjectMocks
    private SessionEventsQueryService service;

    @Test
    @DisplayName("getEvents кидає виняток коли сесія не знайдена")
    void getEvents_throws_whenSessionNotFound() {
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR))
                .thenThrow(new SessionNotFoundException("Not found"));

        assertThatThrownBy(() -> service.getEvents(SESSION_PUBLIC_ID_STR, null, null, null))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("getEvents повертає список подій з маппінгом у DTO")
    void getEvents_returnsMappedDtos() {
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        SessionEvent entity = sessionEvent();
        when(sessionEventRepository.findBySessionUidOrderByLapAscFrameIdAsc(SESSION_UID)).thenReturn(List.of(entity));
        SessionEventDto dto = SessionEventDto.builder().lap(24).eventCode("FTLP").build();
        when(sessionEventMapper.toDto(entity)).thenReturn(dto);

        List<SessionEventDto> result = service.getEvents(SESSION_PUBLIC_ID_STR, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventCode()).isEqualTo("FTLP");
        verify(sessionEventRepository).findBySessionUidOrderByLapAscFrameIdAsc(SESSION_UID);
    }

    @Test
    @DisplayName("getEvents з fromLap викликає фільтр lap >= fromLap")
    void getEvents_withFromLapOnly_usesGreaterThanEqual() {
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        SessionEvent entity = sessionEvent();
        when(sessionEventRepository.findBySessionUidAndLapGreaterThanEqualOrderByLapAscFrameIdAsc(SESSION_UID, (short) 5))
                .thenReturn(List.of(entity));
        SessionEventDto dto = SessionEventDto.builder().lap(24).eventCode("FTLP").build();
        when(sessionEventMapper.toDto(entity)).thenReturn(dto);

        List<SessionEventDto> result = service.getEvents(SESSION_PUBLIC_ID_STR, (short) 5, null, null);

        assertThat(result).hasSize(1);
        verify(sessionEventRepository).findBySessionUidAndLapGreaterThanEqualOrderByLapAscFrameIdAsc(SESSION_UID, (short) 5);
    }

    @Test
    @DisplayName("getEvents з toLap викликає фільтр lap <= toLap")
    void getEvents_withToLapOnly_usesLessThanEqual() {
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        SessionEvent entity = sessionEvent();
        when(sessionEventRepository.findBySessionUidAndLapLessThanEqualOrderByLapAscFrameIdAsc(SESSION_UID, (short) 10))
                .thenReturn(List.of(entity));
        SessionEventDto dto = SessionEventDto.builder().lap(10).eventCode("PENA").build();
        when(sessionEventMapper.toDto(entity)).thenReturn(dto);

        List<SessionEventDto> result = service.getEvents(SESSION_PUBLIC_ID_STR, null, (short) 10, null);

        assertThat(result).hasSize(1);
        verify(sessionEventRepository).findBySessionUidAndLapLessThanEqualOrderByLapAscFrameIdAsc(SESSION_UID, (short) 10);
    }

    @Test
    @DisplayName("getEvents з fromLap і toLap викликає фільтр between")
    void getEvents_withFromLapAndToLap_usesBetween() {
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        SessionEvent entity = sessionEvent();
        when(sessionEventRepository.findBySessionUidAndLapBetweenOrderByLapAscFrameIdAsc(SESSION_UID, (short) 5, (short) 10))
                .thenReturn(List.of(entity));
        SessionEventDto dto = SessionEventDto.builder().lap(7).eventCode("SCAR").build();
        when(sessionEventMapper.toDto(entity)).thenReturn(dto);

        List<SessionEventDto> result = service.getEvents(SESSION_PUBLIC_ID_STR, (short) 5, (short) 10, null);

        assertThat(result).hasSize(1);
        verify(sessionEventRepository).findBySessionUidAndLapBetweenOrderByLapAscFrameIdAsc(SESSION_UID, (short) 5, (short) 10);
    }
}
