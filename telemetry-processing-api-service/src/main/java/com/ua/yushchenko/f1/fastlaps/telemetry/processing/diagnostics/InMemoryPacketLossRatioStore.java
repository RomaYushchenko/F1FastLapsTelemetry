package com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for packet loss ratio per session.
 * Entries older than maxAgeMillis are treated as stale and not returned.
 */
@Component
public class InMemoryPacketLossRatioStore implements PacketLossRatioStore {

    private final long maxAgeMillis;
    private final ConcurrentHashMap<Long, Entry> map = new ConcurrentHashMap<>();

    public InMemoryPacketLossRatioStore(
            @Value("${packet-health.store.max-age-ms:120000}") long maxAgeMillis) {
        this.maxAgeMillis = maxAgeMillis;
    }

    @Override
    public void set(long sessionUid, double packetLossRatio) {
        long now = System.currentTimeMillis();
        map.put(sessionUid, new Entry(packetLossRatio, now));
    }

    @Override
    public Optional<Double> get(long sessionUid) {
        Entry entry = map.get(sessionUid);
        if (entry == null) {
            return Optional.empty();
        }
        long age = System.currentTimeMillis() - entry.timestampMillis;
        if (age > maxAgeMillis) {
            map.remove(sessionUid, entry);
            return Optional.empty();
        }
        return Optional.of(entry.ratio);
    }

    private static final class Entry {
        final double ratio;
        final long timestampMillis;

        Entry(double ratio, long timestampMillis) {
            this.ratio = ratio;
            this.timestampMillis = timestampMillis;
        }
    }
}
