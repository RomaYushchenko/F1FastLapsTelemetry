package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Builds session export payload (summary + laps) as JSON or CSV.
 * Used by GET /api/sessions/{id}/export?format=csv|json.
 * Block I — Step 31.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionExportService {

    private final SessionQueryService sessionQueryService;
    private final LapQueryService lapQueryService;
    private final SessionSummaryQueryService sessionSummaryQueryService;
    private final ObjectMapper objectMapper;

    /**
     * Export format: json or csv.
     */
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_CSV = "csv";

    /**
     * Builds export payload for the given session. Uses player car index from session.
     *
     * @param sessionId public id or session UID
     * @param format    "json" or "csv"
     * @return byte array (UTF-8) of the export body
     */
    public byte[] buildExport(String sessionId, String format) throws IOException {
        log.debug("buildExport: sessionId={}, format={}", sessionId, format);
        SessionDto session = sessionQueryService.getSession(sessionId);
        int carIndex = session.getPlayerCarIndex() != null ? session.getPlayerCarIndex() : 0;
        SessionSummaryDto summary = sessionSummaryQueryService.getSummary(sessionId, (short) carIndex);
        List<LapResponseDto> laps = lapQueryService.getLaps(sessionId, (short) carIndex);

        if (FORMAT_CSV.equalsIgnoreCase(format)) {
            return toCsv(session, summary, laps);
        }
        return toJson(session, summary, laps);
    }

    private byte[] toJson(SessionDto session, SessionSummaryDto summary, List<LapResponseDto> laps) throws IOException {
        Map<String, Object> payload = Map.of(
                "summary", summary != null ? summary : Map.of(),
                "laps", laps != null ? laps : List.of(),
                "session", session != null ? session : Map.of()
        );
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] toCsv(SessionDto session, SessionSummaryDto summary, List<LapResponseDto> laps) {
        StringBuilder sb = new StringBuilder();
        sb.append("lapNumber,lapTimeMs,sector1Ms,sector2Ms,sector3Ms,isInvalid,positionAtLapStart\n");
        if (laps != null) {
            for (LapResponseDto lap : laps) {
                sb.append(lap.getLapNumber()).append(',');
                sb.append(lap.getLapTimeMs() != null ? lap.getLapTimeMs() : "").append(',');
                sb.append(lap.getSector1Ms() != null ? lap.getSector1Ms() : "").append(',');
                sb.append(lap.getSector2Ms() != null ? lap.getSector2Ms() : "").append(',');
                sb.append(lap.getSector3Ms() != null ? lap.getSector3Ms() : "").append(',');
                sb.append(lap.isInvalid()).append(',');
                sb.append(lap.getPositionAtLapStart() != null ? lap.getPositionAtLapStart() : "").append('\n');
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
