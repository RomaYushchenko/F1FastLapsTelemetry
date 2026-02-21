package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for PATCH /api/sessions/{id} — update session display name.
 * Plan: 03-session-page.md Etap 1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSessionDisplayNameRequest {

    /** User-facing display name. Not empty, max 64 characters. */
    @NotBlank(message = "sessionDisplayName must not be blank")
    @Size(max = 64, message = "sessionDisplayName must not exceed 64 characters")
    private String sessionDisplayName;
}
