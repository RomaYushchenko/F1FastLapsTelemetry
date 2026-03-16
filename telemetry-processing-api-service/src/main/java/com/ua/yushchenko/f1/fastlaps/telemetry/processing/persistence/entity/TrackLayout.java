package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.model.Point3D;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.model.SectorBoundary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * 2D track layout (centreline/outline points) for Live Track Map. Block F — B8.
 */
@Entity
@Table(name = "track_layout", schema = "telemetry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackLayout {

    @Id
    @Column(name = "track_id", nullable = false)
    private Short trackId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "points", nullable = false, columnDefinition = "jsonb")
    private List<Point3D> points;

    @Column(name = "version", nullable = false)
    private Short version;

    @Column(name = "min_x")
    private Double minX;

    @Column(name = "min_y")
    private Double minY;

    @Column(name = "max_x")
    private Double maxX;

    @Column(name = "max_y")
    private Double maxY;

    @Column(name = "min_elev")
    private Double minElev;

    @Column(name = "max_elev")
    private Double maxElev;

    @Column(name = "source")
    private String source;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(name = "session_uid")
    private Long sessionUid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sector_boundaries", columnDefinition = "jsonb")
    private List<SectorBoundary> sectorBoundaries;
}
