package com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionNotFoundException")
class SessionNotFoundExceptionTest {

    @Test
    @DisplayName("конструктор з message встановлює message та null cause")
    void constructor_withMessage_setsMessage() {
        // Arrange
        String message = "Session not found: abc";

        // Act
        SessionNotFoundException e = new SessionNotFoundException(message);

        // Assert
        assertThat(e.getMessage()).isEqualTo(message);
        assertThat(e.getCause()).isNull();
    }

    @Test
    @DisplayName("конструктор з message та cause встановлює обидва")
    void constructor_withMessageAndCause_setsBoth() {
        // Arrange
        String message = "Session not found";
        Throwable cause = new IllegalArgumentException("invalid id");

        // Act
        SessionNotFoundException e = new SessionNotFoundException(message, cause);

        // Assert
        assertThat(e.getMessage()).isEqualTo(message);
        assertThat(e.getCause()).isSameAs(cause);
    }
}
