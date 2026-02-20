package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state: last tyre wear (per wheel) per session+car.
 * Updated by CarDamageConsumer; read by TyreWearRecorder when a lap is finalized.
 * Thread-safe.
 */
@Component
public class TyreWearState {

    /** Key: sessionUid + "-" + carIndex */
    private final Map<String, TyreWearSnapshot> lastWear = new ConcurrentHashMap<>();

    public void update(long sessionUid, int carIndex, TyreWearSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        lastWear.put(key(sessionUid, carIndex), snapshot);
    }

    /**
     * Get last wear for this session+car (used when recording for a lap).
     * Returns null if none stored. Snapshot is retained for subsequent laps.
     */
    public TyreWearSnapshot get(long sessionUid, int carIndex) {
        return lastWear.get(key(sessionUid, carIndex));
    }

    private static String key(long sessionUid, int carIndex) {
        return sessionUid + "-" + carIndex;
    }
}
