package com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.ProcessedPacketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProcessedPacketRetentionService")
@ExtendWith(MockitoExtension.class)
class ProcessedPacketRetentionServiceTest {

    @Mock
    private ProcessedPacketRepository processedPacketRepository;

    @InjectMocks
    private ProcessedPacketRetentionService processedPacketRetentionService;

    @Test
    @DisplayName("deleteExpiredBefore делегує в репозиторій і повертає кількість видалених рядків")
    void deleteExpiredBefore_delegatesToRepository() {
        // Arrange
        Instant cutoff = Instant.parse("2025-01-01T00:00:00Z");
        when(processedPacketRepository.deleteByProcessedAtBefore(cutoff)).thenReturn(3);

        // Act
        int deleted = processedPacketRetentionService.deleteExpiredBefore(cutoff);

        // Assert
        assertThat(deleted).isEqualTo(3);
        verify(processedPacketRepository).deleteByProcessedAtBefore(cutoff);
    }
}
