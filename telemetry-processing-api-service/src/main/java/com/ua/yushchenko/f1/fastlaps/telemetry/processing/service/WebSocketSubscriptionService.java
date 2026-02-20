package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErrorCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket subscribe: validate session id, resolve session, check active, register in WebSocketSessionManager.
 * Returns result or error info for client (WsErrorMessage).
 * See: implementation_phases.md Phase 2.4.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSubscriptionService {

    private final SessionResolveService sessionResolveService;
    private final SessionStateManager sessionStateManager;
    private final WebSocketSessionManager wsSessionManager;

    /**
     * Subscribe a WebSocket session to a telemetry session.
     *
     * @param wsSessionId  WebSocket session id
     * @param sessionIdStr Session id (public UUID or session_uid string)
     * @return success, or error with code and message for WsErrorMessage
     */
    public SubscribeResult subscribe(String wsSessionId, String sessionIdStr) {
        if (sessionIdStr == null || sessionIdStr.isBlank()) {
            return SubscribeResult.error(ErrorCode.INVALID_SUBSCRIPTION, "Missing session id");
        }
        Session session;
        try {
            session = sessionResolveService.getSessionByPublicIdOrUid(sessionIdStr.trim());
        } catch (SessionNotFoundException e) {
            return SubscribeResult.error(ErrorCode.SESSION_NOT_FOUND, e.getMessage());
        }
        Long sessionUid = session.getSessionUid();

        SessionRuntimeState state = sessionStateManager.get(sessionUid);
        if (state == null || !state.isActive()) {
            log.warn("Invalid subscription: session {} is not active", sessionUid);
            return SubscribeResult.error(ErrorCode.SESSION_NOT_ACTIVE, "Session is not active");
        }

        wsSessionManager.subscribe(wsSessionId, sessionUid);
        log.info("Live telemetry: client {} subscribed to session {}", wsSessionId, sessionUid);
        return SubscribeResult.ok();
    }

    /**
     * Result of subscribe operation for WebSocket controller to send success or WsErrorMessage.
     */
    public record SubscribeResult(boolean success, ErrorCode errorCode, String message) {
        public static SubscribeResult ok() {
            return new SubscribeResult(true, null, null);
        }

        public static SubscribeResult error(ErrorCode errorCode, String message) {
            return new SubscribeResult(false, errorCode, message);
        }
    }
}
