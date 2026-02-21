package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for SessionFinishingPosition entity.
 * Plan: 03-session-page.md Etap 3.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionFinishingPositionId implements Serializable {

    private Long sessionUid;
    private Short carIndex;
}
