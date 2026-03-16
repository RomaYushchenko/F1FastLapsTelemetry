package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantDataDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantsEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_UID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipantsConsumer")
class ParticipantsConsumerTest {

    @Mock
    private SessionStateManager sessionStateManager;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private SessionRuntimeState runtimeState;

    @InjectMocks
    private ParticipantsConsumer consumer;

    @Test
    @DisplayName("consume оновлює учасників і викликає ack коли payload валідний")
    void consume_updatesParticipantsAndAcknowledges_whenPayloadValid() {
        // Arrange
        ParticipantsEvent event = new ParticipantsEvent();
        event.setSessionUID(SESSION_UID);

        ParticipantDataDto p0 = new ParticipantDataDto();
        p0.setCarIndex(0);
        p0.setRaceNumber(1);
        p0.setName("VER");

        ParticipantDataDto p1 = new ParticipantDataDto();
        p1.setCarIndex(1);
        p1.setRaceNumber(11);
        p1.setName("PER");

        ParticipantsDto payload = new ParticipantsDto();
        payload.setParticipants(List.of(p0, p1));
        event.setPayload(payload);

        when(sessionStateManager.getOrCreate(SESSION_UID)).thenReturn(runtimeState);

        // Act
        consumer.consume(event, acknowledgment);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ParticipantDataDto>> captor = ArgumentCaptor.forClass((Class<List<ParticipantDataDto>>) (Class<?>) List.class);
        verify(runtimeState).setParticipants(captor.capture());
        List<ParticipantDataDto> captured = captor.getValue();
        assertThat(captured).hasSize(2);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("consume пропускає коли payload або participants null і викликає ack")
    void consume_skipsWhenPayloadEmpty() {
        // Arrange
        ParticipantsEvent event = new ParticipantsEvent();
        event.setSessionUID(SESSION_UID);
        event.setPayload(null);

        // Act
        consumer.consume(event, acknowledgment);

        // Assert
        verify(sessionStateManager, never()).getOrCreate(anyLong());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("consume з event == null логічно завершується та викликає ack")
    void consume_handlesNullEvent() {
        // Act
        consumer.consume(null, acknowledgment);

        // Assert
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(sessionStateManager);
    }
}

