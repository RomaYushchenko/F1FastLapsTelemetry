package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Composite PK for LapCornerMetrics (session_uid, car_index, lap_number, corner_index). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LapCornerMetricsId implements Serializable {

    private Long sessionUid;
    private Short carIndex;
    private Short lapNumber;
    private Short cornerIndex;
}
