package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for tyre wear chart (GET /api/sessions/{sessionUid}/tyre-wear).
 * One point per lap: lap number, wear per wheel (FL, FR, RL, RR), and optional compound.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TyreWearPointDto {

    private int lapNumber;
    private Float wearFL;
    private Float wearFR;
    private Float wearRL;
    private Float wearRR;
    /** F1 25 actual tyre compound at end of lap (e.g. 16=C5, 18=C3, 7=inter, 8=wet). Optional. */
    private Integer compound;
}
