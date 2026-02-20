package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Raw car status sample (telemetry.car_status_raw).
 * See: implementation_steps_plan.md § 2.8, 6.6; infra/init-db/06-car-status-raw.sql, 13-car-status-tyres-age-laps.sql.
 */
@Entity
@Table(name = "car_status_raw", schema = "telemetry")
@IdClass(CarStatusRawId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarStatusRaw {

    @Id
    @Column(name = "ts", nullable = false)
    private Instant ts;

    @Id
    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Id
    @Column(name = "frame_identifier", nullable = false)
    private Integer frameIdentifier;

    @Id
    @Column(name = "car_index", nullable = false)
    private Short carIndex;

    @Column(name = "traction_control")
    private Short tractionControl;

    @Column(name = "abs")
    private Short abs;

    @Column(name = "fuel_in_tank")
    private Float fuelInTank;

    @Column(name = "fuel_mix")
    private Short fuelMix;

    @Column(name = "drs_allowed")
    private Boolean drsAllowed;

    @Column(name = "tyres_compound")
    private Short tyresCompound;

    @Column(name = "tyres_age_laps")
    private Short tyresAgeLaps;

    @Column(name = "ers_store_energy")
    private Float ersStoreEnergy;

    @Column(name = "session_time_s")
    private Float sessionTimeS;
}
