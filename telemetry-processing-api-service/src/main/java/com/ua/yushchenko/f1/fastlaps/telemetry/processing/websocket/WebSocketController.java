package com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsErrorMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSubscribeMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsUnsubscribeMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErrorCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionResolveService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket message handler for STOMP client messages.
 * Session id in SUBSCRIBE must match REST: UUID (public_id) or numeric (session_uid).
 * See: implementation_steps_plan.md § Етап 9.3.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketSessionManager wsSessionManager;
    private final SessionStateManager sessionStateManager;
    private final SessionResolveService sessionResolveService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle SUBSCRIBE message from client.
     * Client sends session id (same as REST: UUID or session_uid string). We resolve and subscribe.
     */
    @MessageMapping("/subscribe")
    public void handleSubscribe(
            @Payload WsSubscribeMessage message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String wsSessionId = headerAccessor.getSessionId();
        String sessionIdStr = message.getSessionId();
        if (sessionIdStr == null || sessionIdStr.isBlank()) {
            sendError(wsSessionId, ErrorCode.INVALID_SUBSCRIPTION, "Missing session id");
            return;
        }
        Session session;
        try {
            session = sessionResolveService.getSessionByPublicIdOrUid(sessionIdStr.trim());
        } catch (SessionNotFoundException e) {
            sendError(wsSessionId, ErrorCode.SESSION_NOT_FOUND, e.getMessage());
            return;
        }
        Long sessionUid = session.getSessionUid();

        log.info("Live telemetry: SUBSCRIBE request from wsSession={}, sessionId={}", wsSessionId, sessionIdStr.trim());

        SessionRuntimeState state = sessionStateManager.get(sessionUid);
        if (state == null || !state.isActive()) {
            log.warn("Invalid subscription: session {} is not active", sessionUid);
            sendError(wsSessionId, ErrorCode.SESSION_NOT_ACTIVE, "Session is not active");
            return;
        }

        wsSessionManager.subscribe(wsSessionId, sessionUid);
        log.info("Live telemetry: client {} subscribed to session {}", wsSessionId, sessionUid);
    }

    /**
     * Handle UNSUBSCRIBE message from client.
     * Client sends to /app/unsubscribe, we process and remove tracking.
     */
    @MessageMapping("/unsubscribe")
    public void handleUnsubscribe(
            @Payload WsUnsubscribeMessage message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String wsSessionId = headerAccessor.getSessionId();

        log.debug("UNSUBSCRIBE request: wsSession={}", wsSessionId);

        // Unsubscribe client
        wsSessionManager.unsubscribe(wsSessionId);

        log.info("Live telemetry: client {} unsubscribed", wsSessionId);
    }

    /**
     * Send error message to a specific WebSocket client.
     */
    private void sendError(String wsSessionId, ErrorCode errorCode, String errorMessage) {
        WsErrorMessage error = WsErrorMessage.builder()
                .type(WsErrorMessage.TYPE)
                .code(errorCode.name())
                .message(errorMessage)
                .build();

        messagingTemplate.convertAndSendToUser(
                wsSessionId,
                "/queue/errors",
                error
        );
    }
}
