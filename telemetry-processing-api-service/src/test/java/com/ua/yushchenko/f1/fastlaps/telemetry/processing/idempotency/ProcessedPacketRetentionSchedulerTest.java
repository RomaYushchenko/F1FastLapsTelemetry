package com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.ProcessedPacketRetentionProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProcessedPacketRetentionScheduler")
@ExtendWith(MockitoExtension.class)
class ProcessedPacketRetentionSchedulerTest {

    @Mock
    private ProcessedPacketRetentionService processedPacketRetentionService;

    @Mock
    private ProcessedPacketRetentionProperties properties;

    @InjectMocks
    private ProcessedPacketRetentionScheduler processedPacketRetentionScheduler;

    @Test
    @DisplayName("purgeExpired викликає deleteExpiredBefore з cutoff приблизно now мінус retention")
    void purgeExpired_passesCutoffBasedOnRetention() {
        // Arrange
        Duration retention = Duration.ofMinutes(2);
        when(properties.getRetention()).thenReturn(retention);
        when(processedPacketRetentionService.deleteExpiredBefore(any())).thenReturn(0);

        // Act
        processedPacketRetentionScheduler.purgeExpired();

        // Assert
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(processedPacketRetentionService).deleteExpiredBefore(cutoffCaptor.capture());
        Instant cutoff = cutoffCaptor.getValue();
        assertThat(cutoff.isBefore(Instant.now())).isTrue();
        long secondsBetween = Duration.between(cutoff, Instant.now()).getSeconds();
        assertThat(secondsBetween).isBetween(119L, 125L);
    }
}
