package com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsErrorMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSubscribeMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsUnsubscribeMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErrorCode;
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
 * See: implementation_steps_plan.md § Етап 9.3.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketSessionManager wsSessionManager;
    private final SessionStateManager sessionStateManager;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle SUBSCRIBE message from client.
     * Client sends to /app/subscribe, we process and track subscription.
     */
    @MessageMapping("/subscribe")
    public void handleSubscribe(
            @Payload WsSubscribeMessage message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String wsSessionId = headerAccessor.getSessionId();
        Long sessionUid = message.getSessionUID();

        log.debug("SUBSCRIBE request: wsSession={}, telemetrySession={}", wsSessionId, sessionUid);

        // Validate session exists and is active
        SessionRuntimeState state = sessionStateManager.get(sessionUid);
        if (state == null || !state.isActive()) {
            log.warn("Invalid subscription: session {} is not active", sessionUid);
            sendError(wsSessionId, ErrorCode.SESSION_NOT_ACTIVE, "Session is not active");
            return;
        }

        // Subscribe client
        wsSessionManager.subscribe(wsSessionId, sessionUid);

        log.info("WebSocket client {} subscribed to session {}", wsSessionId, sessionUid);
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

        log.info("WebSocket client {} unsubscribed", wsSessionId);
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
