package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Composite primary key for CarStatusRaw (telemetry.car_status_raw).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarStatusRawId implements Serializable {

    private Instant ts;
    private Long sessionUid;
    private Integer frameIdentifier;
    private Short carIndex;
}
