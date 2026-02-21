package com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSessionEndedMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSnapshotMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder.WsSnapshotMessageBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionQueryService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Broadcasts live telemetry snapshots to WebSocket subscribers at 10 Hz.
 * See: implementation_steps_plan.md § Етап 9.4.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveDataBroadcaster {

    private final SessionStateManager sessionStateManager;
    private final WebSocketSessionManager wsSessionManager;
    private final SessionQueryService sessionQueryService;
    private final SessionSummaryRepository sessionSummaryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast snapshots every 100ms (10 Hz) for all active sessions with subscribers.
     * Enriches snapshot with bestLapTimeMs from SessionSummary for delta-to-best display.
     */
    @Scheduled(fixedRate = 100)
    public void broadcastSnapshots() {
        Map<Long, SessionRuntimeState> activeSessions = sessionStateManager.getAllActive();

        for (Map.Entry<Long, SessionRuntimeState> entry : activeSessions.entrySet()) {
            Long sessionUid = entry.getKey();
            SessionRuntimeState state = entry.getValue();

            // Only broadcast if someone is subscribed
            if (!wsSessionManager.hasSubscribers(sessionUid)) {
                continue;
            }

            SessionRuntimeState.CarSnapshot carSnapshot = state.getLatestCarSnapshot();
            if (carSnapshot == null) {
                continue; // No data yet
            }

            // Enrich with best lap time from SessionSummary for delta calculation
            short carIndex = state.getPlayerCarIndex() != null ? state.getPlayerCarIndex().shortValue() : 0;
            sessionSummaryRepository.findBySessionUidAndCarIndex(sessionUid, carIndex)
                    .map(s -> s.getBestLapTimeMs())
                    .ifPresent(carSnapshot::setBestLapTimeMs);

            WsSnapshotMessage snapshot = WsSnapshotMessageBuilder.build(carSnapshot);
            if (snapshot == null) {
                continue;
            }

            // Use same id as REST (public_id or session_uid) so client topic matches SessionDto.id
            String topicId = sessionQueryService.getTopicIdForSession(sessionUid).orElse(null);
            if (topicId == null) {
                continue;
            }
            String destination = "/topic/live/" + topicId;
            messagingTemplate.convertAndSend(destination, snapshot);

            log.trace("Broadcast snapshot for session {}: {} subscribers",
                    sessionUid, wsSessionManager.getSubscriberCount(sessionUid));
        }
    }

    /**
     * Send SESSION_ENDED notification to all subscribers of a session.
     *
     * @param sessionUid Session that ended
     * @param reason End reason
     */
    public void notifySessionEnded(Long sessionUid, String reason) {
        if (!wsSessionManager.hasSubscribers(sessionUid)) {
            return; // No subscribers, skip
        }

        String topicId = sessionQueryService.getTopicIdForSession(sessionUid).orElse(null);
        if (topicId == null) {
            return;
        }
        WsSessionEndedMessage message = WsSessionEndedMessage.builder()
                .type(WsSessionEndedMessage.TYPE)
                .sessionId(topicId)
                .endReason(reason)
                .build();

        String destination = "/topic/live/" + topicId;
        messagingTemplate.convertAndSend(destination, message);

        log.info("Sent SESSION_ENDED notification for session {} to {} subscribers (reason: {})",
                sessionUid, wsSessionManager.getSubscriberCount(sessionUid), reason);
    }
}
