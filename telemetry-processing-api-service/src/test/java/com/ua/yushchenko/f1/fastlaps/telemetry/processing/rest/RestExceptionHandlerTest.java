package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErrorCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.RestErrorResponse;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestExceptionHandler")
class RestExceptionHandlerTest {

    private RestExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler();
    }

    @Test
    @DisplayName("handleSessionNotFound повертає 404 та SESSION_NOT_FOUND")
    void handleSessionNotFound_returns404AndSessionNotFoundCode() {
        // Arrange
        SessionNotFoundException e = new SessionNotFoundException("Session not found: xyz");

        // Act
        ResponseEntity<RestErrorResponse> response = handler.handleSessionNotFound(e);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo(ErrorCode.SESSION_NOT_FOUND.name());
        assertThat(response.getBody().getMessage()).isEqualTo("Session not found: xyz");
    }

    @Test
    @DisplayName("handleIllegalArgument повертає 400 та INVALID_REQUEST")
    void handleIllegalArgument_returns400AndInvalidRequestCode() {
        // Arrange
        IllegalArgumentException e = new IllegalArgumentException("Invalid offset");

        // Act
        ResponseEntity<RestErrorResponse> response = handler.handleIllegalArgument(e);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo(ErrorCode.INVALID_REQUEST.name());
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid offset");
    }

    @Test
    @DisplayName("handleMethodArgumentNotValid повертає 400 та INVALID_REQUEST для невалідного body")
    void handleMethodArgumentNotValid_returns400AndInvalidRequestCode() throws NoSuchMethodException {
        // Arrange: simulate @Valid failure on sessionDisplayName (blank or too long)
        Method method = SessionController.class.getMethod("updateSessionDisplayName", String.class,
                UpdateSessionDisplayNameRequest.class);
        MethodParameter parameter = new MethodParameter(method, 1);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(null, "request");
        bindingResult.addError(new FieldError("request", "sessionDisplayName", "must not be blank"));
        MethodArgumentNotValidException e = new MethodArgumentNotValidException(parameter, bindingResult);

        // Act
        ResponseEntity<RestErrorResponse> response = handler.handleMethodArgumentNotValid(e);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo(ErrorCode.INVALID_REQUEST.name());
        assertThat(response.getBody().getMessage()).contains("sessionDisplayName");
        assertThat(response.getBody().getMessage()).contains("must not be blank");
    }

    @Test
    @DisplayName("handleGenericException повертає 500 та INTERNAL_ERROR")
    void handleGenericException_returns500AndInternalErrorCode() {
        // Arrange
        Exception e = new RuntimeException("Unexpected");

        // Act
        ResponseEntity<RestErrorResponse> response = handler.handleGenericException(e);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo(ErrorCode.INTERNAL_ERROR.name());
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
    }
}
