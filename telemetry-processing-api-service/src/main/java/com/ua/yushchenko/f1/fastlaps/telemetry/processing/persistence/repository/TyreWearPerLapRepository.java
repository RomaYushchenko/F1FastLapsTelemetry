package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLapId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TyreWearPerLapRepository extends JpaRepository<TyreWearPerLap, TyreWearPerLapId> {

    List<TyreWearPerLap> findBySessionUidAndCarIndexOrderByLapNumberAsc(Long sessionUid, Short carIndex);
}
