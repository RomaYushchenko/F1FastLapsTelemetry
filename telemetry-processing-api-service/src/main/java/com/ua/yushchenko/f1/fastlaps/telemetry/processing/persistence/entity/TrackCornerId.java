package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Composite PK for TrackCorner (map_id, corner_index). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackCornerId implements Serializable {

    private Long mapId;
    private Short cornerIndex;
}
