package com.ua.yushchenko.f1.fastlaps.telemetry.processing.corner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SteerBasedCornerSegmenter")
class SteerBasedCornerSegmenterTest {

    private SteerBasedCornerSegmenter segmenter;

    @BeforeEach
    void setUp() {
        segmenter = new SteerBasedCornerSegmenter();
    }

    @Test
    @DisplayName("detect повертає порожній список коли точок мало")
    void detect_returnsEmpty_whenFewPoints() {
        // Arrange
        List<SteerBasedCornerSegmenter.Point> points = List.of(
                new SteerBasedCornerSegmenter.Point(0f, 200, 0.02f)
        );

        // Act
        List<CornerSegment> result = segmenter.detect(points);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("detect знаходить один сегмент при steer > порога")
    void detect_findsOneSegment_whenSteerAboveThreshold() {
        // Arrange: straight then corner (high |steer|) then straight. Min segment length 20m.
        List<SteerBasedCornerSegmenter.Point> points = List.of(
                new SteerBasedCornerSegmenter.Point(0f, 280, 0.01f),
                new SteerBasedCornerSegmenter.Point(50f, 250, 0.08f),
                new SteerBasedCornerSegmenter.Point(100f, 140, 0.12f),
                new SteerBasedCornerSegmenter.Point(150f, 180, 0.06f),
                new SteerBasedCornerSegmenter.Point(200f, 220, 0.02f)
        );

        // Act
        List<CornerSegment> result = segmenter.detect(points);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStartDistanceM()).isEqualTo(50f);
        assertThat(result.get(0).getEndDistanceM()).isEqualTo(200f);
        assertThat(result.get(0).getApexDistanceM()).isEqualTo(100f);
    }

    @Test
    @DisplayName("detect повертає порожній список коли steer завжди нижче порога")
    void detect_returnsEmpty_whenSteerAlwaysLow() {
        // Arrange
        List<SteerBasedCornerSegmenter.Point> points = List.of(
                new SteerBasedCornerSegmenter.Point(0f, 200, 0.01f),
                new SteerBasedCornerSegmenter.Point(100f, 180, 0.02f)
        );

        // Act
        List<CornerSegment> result = segmenter.detect(points);

        // Assert
        assertThat(result).isEmpty();
    }
}
