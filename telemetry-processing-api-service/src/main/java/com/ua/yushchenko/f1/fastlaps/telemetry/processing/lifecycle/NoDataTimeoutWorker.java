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
 * Runs periodically and triggers session end if no data received for timeout period.
 * See: implementation_steps_plan.md § Етап 4.9.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoDataTimeoutWorker {

    private final SessionStateManager stateManager;
    private final SessionLifecycleService lifecycleService;

    @Value("${telemetry.session.timeout-seconds:30}")
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
