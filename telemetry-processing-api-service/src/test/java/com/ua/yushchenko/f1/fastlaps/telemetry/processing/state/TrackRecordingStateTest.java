package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TrackRecordingState")
class TrackRecordingStateTest {

    @Test
    @DisplayName("shouldSample returns true every third frame")
    void shouldSample_everyThirdFrame() {
        // Arrange
        TrackRecordingState state = new TrackRecordingState();

        // Act & Assert
        assertThat(state.shouldSample()).isFalse(); // 1
        assertThat(state.shouldSample()).isFalse(); // 2
        assertThat(state.shouldSample()).isTrue();  // 3
        assertThat(state.shouldSample()).isFalse(); // 4
        assertThat(state.shouldSample()).isFalse(); // 5
        assertThat(state.shouldSample()).isTrue();  // 6
    }

    @Test
    @DisplayName("addPoint appends sampled point to buffer")
    void addPoint_appendsToBuffer() {
        // Arrange
        TrackRecordingState state = new TrackRecordingState();

        // Act
        state.addPoint(1.0f, 2.0f, 3.0f, 10.5f);
        state.addPoint(4.0f, 5.0f, 6.0f, 20.5f);

        // Assert
        assertThat(state.getBuffer()).hasSize(2);
        assertThat(state.getBuffer().get(0).x()).isEqualTo(1.0f);
        assertThat(state.getBuffer().get(0).lapDistance()).isEqualTo(10.5f);
        assertThat(state.getBuffer().get(1).x()).isEqualTo(4.0f);
        assertThat(state.getBuffer().get(1).lapDistance()).isEqualTo(20.5f);
    }

    @Test
    @DisplayName("reset clears buffer and sampling counters")
    void reset_clearsState() {
        // Arrange
        TrackRecordingState state = new TrackRecordingState();
        state.addPoint(1.0f, 2.0f, 3.0f, 10.5f);
        state.shouldSample();
        state.shouldSample();

        // Act
        state.reset();

        // Assert
        assertThat(state.getBuffer()).isEmpty();
        // After reset the next frame should be treated as first again
        assertThat(state.shouldSample()).isFalse(); // 1
        assertThat(state.shouldSample()).isFalse(); // 2
        assertThat(state.shouldSample()).isTrue();  // 3
    }
}

