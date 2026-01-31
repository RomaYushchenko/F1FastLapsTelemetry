package com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages WebSocket client subscriptions to sessions.
 * Thread-safe for concurrent access.
 * See: implementation_steps_plan.md § Етап 9.2.
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * Map of sessionUID -> set of WebSocket session IDs subscribed to that session.
     */
    private final Map<Long, Set<String>> sessionSubscribers = new ConcurrentHashMap<>();

    /**
     * Map of WebSocket session ID -> sessionUID they're subscribed to.
     */
    private final Map<String, Long> clientSessions = new ConcurrentHashMap<>();

    /**
     * Subscribe a WebSocket client to a telemetry session.
     *
     * @param wsSessionId WebSocket session ID
     * @param sessionUid Telemetry session UID
     */
    public void subscribe(String wsSessionId, Long sessionUid) {
        // Unsubscribe from any previous session
        unsubscribe(wsSessionId);

        // Add to session subscribers
        sessionSubscribers.computeIfAbsent(sessionUid, k -> new CopyOnWriteArraySet<>())
                .add(wsSessionId);

        // Track client subscription
        clientSessions.put(wsSessionId, sessionUid);

        log.info("WebSocket session {} subscribed to telemetry session {}", wsSessionId, sessionUid);
    }

    /**
     * Unsubscribe a WebSocket client from their current session.
     *
     * @param wsSessionId WebSocket session ID
     */
    public void unsubscribe(String wsSessionId) {
        Long sessionUid = clientSessions.remove(wsSessionId);
        if (sessionUid != null) {
            Set<String> subscribers = sessionSubscribers.get(sessionUid);
            if (subscribers != null) {
                subscribers.remove(wsSessionId);
                if (subscribers.isEmpty()) {
                    sessionSubscribers.remove(sessionUid);
                }
            }
            log.info("WebSocket session {} unsubscribed from telemetry session {}", wsSessionId, sessionUid);
        }
    }

    /**
     * Get all WebSocket sessions subscribed to a telemetry session.
     *
     * @param sessionUid Telemetry session UID
     * @return Set of WebSocket session IDs (empty if none)
     */
    public Set<String> getSubscribers(Long sessionUid) {
        return sessionSubscribers.getOrDefault(sessionUid, Set.of());
    }

    /**
     * Get the telemetry session a WebSocket client is subscribed to.
     *
     * @param wsSessionId WebSocket session ID
     * @return Telemetry session UID, or null if not subscribed
     */
    public Long getSubscribedSession(String wsSessionId) {
        return clientSessions.get(wsSessionId);
    }

    /**
     * Check if anyone is subscribed to a telemetry session.
     *
     * @param sessionUid Telemetry session UID
     * @return true if at least one subscriber exists
     */
    public boolean hasSubscribers(Long sessionUid) {
        Set<String> subscribers = sessionSubscribers.get(sessionUid);
        return subscribers != null && !subscribers.isEmpty();
    }

    /**
     * Get count of subscribers for a session.
     *
     * @param sessionUid Telemetry session UID
     * @return Number of subscribers
     */
    public int getSubscriberCount(Long sessionUid) {
        Set<String> subscribers = sessionSubscribers.get(sessionUid);
        return subscribers != null ? subscribers.size() : 0;
    }
}
