package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.MotionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.MotionEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.TrackLayoutRecordingService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.CAR_INDEX;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.FRAME_ID;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_UID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MotionConsumer")
class MotionConsumerTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private SessionLifecycleService lifecycleService;

    @Mock
    private SessionStateManager sessionStateManager;

    @Mock
    private TrackLayoutRecordingService trackLayoutRecordingService;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private SessionRuntimeState runtimeState;

    @InjectMocks
    private MotionConsumer consumer;

    @Test
    @DisplayName("consume оновлює позицію та викликає onMotionFrame при валідному payload")
    void consume_updatesStateAndRecording_whenPayloadValid() {
        // Arrange
        MotionEvent event = new MotionEvent();
        event.setSessionUID(SESSION_UID);
        event.setFrameIdentifier(FRAME_ID);
        event.setPacketId(PacketId.MOTION);
        event.setCarIndex(CAR_INDEX);

        MotionDto dto = new MotionDto();
        dto.setWorldPositionX(10.0f);
        dto.setWorldPositionY(1.5f);
        dto.setWorldPositionZ(20.0f);
        dto.setGForceLateral(0.5f);
        dto.setYaw(0.1f);
        event.setPayload(dto);

        when(lifecycleService.shouldProcessPacket(SESSION_UID)).thenReturn(true);
        when(idempotencyService.markAsProcessed(eq(SESSION_UID), eq(FRAME_ID), anyShort(), eq(CAR_INDEX))).thenReturn(true);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        when(runtimeState.getLatestLapDistance(CAR_INDEX)).thenReturn(123.45f);

        // Act
        consumer.consume(event, acknowledgment);

        // Assert
        verify(lifecycleService).ensureSessionActive(SESSION_UID);
        verify(runtimeState).updatePosition(CAR_INDEX, 10.0f, 20.0f);
        verify(trackLayoutRecordingService).onMotionFrame(SESSION_UID, CAR_INDEX, 10.0f, 1.5f, 20.0f, 123.45f);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("consume пропускає пакет коли shouldProcessPacket=false")
    void consume_skipsWhenShouldNotProcess() {
        // Arrange
        MotionEvent event = new MotionEvent();
        event.setSessionUID(SESSION_UID);
        event.setFrameIdentifier(FRAME_ID);
        event.setPacketId(PacketId.MOTION);
        event.setCarIndex(CAR_INDEX);
        event.setPayload(new MotionDto());

        when(lifecycleService.shouldProcessPacket(SESSION_UID)).thenReturn(false);

        // Act
        consumer.consume(event, acknowledgment);

        // Assert
        verify(idempotencyService, never()).markAsProcessed(anyLong(), anyInt(), anyShort(), anyShort());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("consume пропускає дублікат коли markAsProcessed повертає false")
    void consume_skipsDuplicateWhenIdempotencyFails() {
        // Arrange
        MotionEvent event = new MotionEvent();
        event.setSessionUID(SESSION_UID);
        event.setFrameIdentifier(FRAME_ID);
        event.setPacketId(PacketId.MOTION);
        event.setCarIndex(CAR_INDEX);
        event.setPayload(new MotionDto());

        when(lifecycleService.shouldProcessPacket(SESSION_UID)).thenReturn(true);
        when(idempotencyService.markAsProcessed(eq(SESSION_UID), eq(FRAME_ID), anyShort(), eq(CAR_INDEX))).thenReturn(false);

        // Act
        consumer.consume(event, acknowledgment);

        // Assert
        verify(trackLayoutRecordingService, never()).onMotionFrame(anyLong(), anyShort(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("consume обробляє event == null і викликає ack")
    void consume_handlesNullEvent() {
        // Act
        consumer.consume(null, acknowledgment);

        // Assert
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(idempotencyService, lifecycleService, sessionStateManager, trackLayoutRecordingService);
    }
}
