package com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsErrorMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSubscribeMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsUnsubscribeMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErrorCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.WebSocketSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket message handler for STOMP client messages.
 * Thin: delegates subscribe to WebSocketSubscriptionService; on error sends WsErrorMessage.
 * See: implementation_phases.md Phase 3.2.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketSessionManager wsSessionManager;
    private final WebSocketSubscriptionService subscriptionService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/subscribe")
    public void handleSubscribe(
            @Payload WsSubscribeMessage message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String wsSessionId = headerAccessor.getSessionId();
        String sessionIdStr = message.getSessionId();

        log.info("Live telemetry: SUBSCRIBE request from wsSession={}, sessionId={}", wsSessionId, sessionIdStr);

        WebSocketSubscriptionService.SubscribeResult result = subscriptionService.subscribe(wsSessionId, sessionIdStr);
        if (!result.success()) {
            sendError(wsSessionId, result.errorCode(), result.message());
        }
    }

    @MessageMapping("/unsubscribe")
    public void handleUnsubscribe(
            @Payload WsUnsubscribeMessage message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String wsSessionId = headerAccessor.getSessionId();
        log.debug("UNSUBSCRIBE request: wsSession={}", wsSessionId);
        wsSessionManager.unsubscribe(wsSessionId);
        log.info("Live telemetry: client {} unsubscribed", wsSessionId);
    }

    private void sendError(String wsSessionId, ErrorCode errorCode, String errorMessage) {
        WsErrorMessage error = WsErrorMessage.builder()
                .type(WsErrorMessage.TYPE)
                .code(errorCode.name())
                .message(errorMessage)
                .build();
        messagingTemplate.convertAndSendToUser(wsSessionId, "/queue/errors", error);
    }
}
