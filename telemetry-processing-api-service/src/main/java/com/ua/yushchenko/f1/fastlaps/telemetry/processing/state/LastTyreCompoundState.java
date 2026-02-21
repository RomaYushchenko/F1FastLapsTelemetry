package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state: last known actual tyre compound (F1 25 code) per session+car.
 * Updated by CarStatusProcessor; read by TyreWearRecorder when a lap is finalized.
 * Thread-safe.
 */
@Component
public class LastTyreCompoundState {

    /** Key: sessionUid + "-" + carIndex; value: F1 25 m_actualTyreCompound (e.g. 16=C5, 18=C3, 7=inter, 8=wet). */
    private final Map<String, Short> lastCompound = new ConcurrentHashMap<>();

    public void update(long sessionUid, int carIndex, Integer compound) {
        if (compound == null) {
            return;
        }
        short value = compound.shortValue();
        if (value < 0) {
            return;
        }
        lastCompound.put(key(sessionUid, carIndex), value);
    }

    /**
     * Get last compound for this session+car (used when recording tyre wear for a lap).
     */
    public Short get(long sessionUid, int carIndex) {
        return lastCompound.get(key(sessionUid, carIndex));
    }

    private static String key(long sessionUid, int carIndex) {
        return sessionUid + "-" + carIndex;
    }
}
