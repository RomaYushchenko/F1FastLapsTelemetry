package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tyre wear per lap (telemetry.tyre_wear_per_lap table).
 * One row per (session, car, lap) with wear percentage per wheel.
 */
@Entity
@Table(name = "tyre_wear_per_lap", schema = "telemetry")
@IdClass(TyreWearPerLapId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TyreWearPerLap {

    @Id
    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Id
    @Column(name = "car_index", nullable = false)
    private Short carIndex;

    @Id
    @Column(name = "lap_number", nullable = false)
    private Short lapNumber;

    @Column(name = "wear_fl")
    private Float wearFL;

    @Column(name = "wear_fr")
    private Float wearFR;

    @Column(name = "wear_rl")
    private Float wearRL;

    @Column(name = "wear_rr")
    private Float wearRR;

    /** F1 25 actual tyre compound at end of lap (e.g. 16=C5, 18=C3, 7=inter, 8=wet). */
    @Column(name = "compound")
    private Short compound;
}
