package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Composite PK for SessionDriver. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDriverId implements Serializable {

    private Long sessionUid;
    private Short carIndex;
}
