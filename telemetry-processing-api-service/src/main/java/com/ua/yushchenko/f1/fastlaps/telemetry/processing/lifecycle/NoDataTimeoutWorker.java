package com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Background worker that checks for session timeouts.
 * Session is ended only if no telemetry at all for the timeout period (e.g. game closed).
 * Safety car, pit stops, or brief gaps do not end the session as long as any packet
 * (lap data, car telemetry, car status, or session data) is received within the window.
 * See: implementation_steps_plan.md § Етап 4.9.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoDataTimeoutWorker {

    private final SessionStateManager stateManager;
    private final SessionLifecycleService lifecycleService;

    /** No data for this many seconds triggers session end. Default 60s to tolerate safety car / pit / pauses. */
    @Value("${telemetry.session.timeout-seconds:60}")
    private int timeoutSeconds;

    /**
     * Check for session timeouts every 10 seconds.
     */
    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    public void checkTimeouts() {
        Map<Long, SessionRuntimeState> activeSessions = stateManager.getAllActive();

        if (activeSessions.isEmpty()) {
            return; // No active sessions, skip
        }

        Instant now = Instant.now();
        Duration timeout = Duration.ofSeconds(timeoutSeconds);

        activeSessions.forEach((sessionUID, state) -> {
            Instant lastSeen = state.getLastSeenAt();
            if (lastSeen == null) {
                return; // Should not happen, but be defensive
            }

            Duration elapsed = Duration.between(lastSeen, now);
            if (elapsed.compareTo(timeout) > 0) {
                log.warn("Session timeout: sessionUID={}, lastSeen={}, elapsed={}s",
                        sessionUID, lastSeen, elapsed.toSeconds());
                lifecycleService.onSessionTimeout(sessionUID);
            }
        });
    }
}
