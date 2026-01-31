package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages runtime state for all active sessions (in-memory).
 * Thread-safe singleton that provides access to SessionRuntimeState instances.
 * See: implementation_steps_plan.md § Етап 4.4–4.5.
 */
@Slf4j
@Component
public class SessionStateManager {

    private final Map<Long, SessionRuntimeState> sessions = new ConcurrentHashMap<>();

    /**
     * Get or create runtime state for a session.
     * Thread-safe: creates state atomically if not exists.
     */
    public SessionRuntimeState getOrCreate(long sessionUID) {
        return sessions.computeIfAbsent(sessionUID, uid -> {
            log.info("Creating new SessionRuntimeState for sessionUID={}", uid);
            return new SessionRuntimeState(uid);
        });
    }

    /**
     * Get existing session state (returns null if not found).
     */
    public SessionRuntimeState get(long sessionUID) {
        return sessions.get(sessionUID);
    }

    /**
     * Close session: transition to TERMINAL and optionally remove from map.
     * For MVP, we keep sessions in memory for history/debugging.
     */
    public void close(long sessionUID) {
        SessionRuntimeState state = sessions.get(sessionUID);
        if (state != null) {
            log.info("Closing session sessionUID={}, state={}", sessionUID, state.getState());
            state.transitionTo(SessionState.TERMINAL);
            // Optional: sessions.remove(sessionUID); // Remove to free memory
        }
    }

    /**
     * Get all active sessions (not TERMINAL).
     */
    public Map<Long, SessionRuntimeState> getAllActive() {
        Map<Long, SessionRuntimeState> active = new ConcurrentHashMap<>();
        sessions.forEach((uid, state) -> {
            if (!state.isTerminal()) {
                active.put(uid, state);
            }
        });
        return active;
    }

    /**
     * Get count of active sessions.
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
                .filter(state -> !state.isTerminal())
                .count();
    }
}
