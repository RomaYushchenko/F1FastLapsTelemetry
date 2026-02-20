package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TyreWearPerLapId implements Serializable {

    private Long sessionUid;
    private Short carIndex;
    private Short lapNumber;
}
