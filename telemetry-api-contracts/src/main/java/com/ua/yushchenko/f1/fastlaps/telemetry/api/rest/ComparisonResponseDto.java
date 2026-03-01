package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * REST DTO for Driver Comparison (GET /api/sessions/{sessionUid}/comparison).
 * Contains laps, summary, pace, trace and speed trace for two cars (A and B).
 * referenceLapNumA/B are the lap numbers used for trace/speed (default: best lap per car).
 * See: block-g-driver-comparison.md § 4, rest_web_socket_api_contracts § 3.6.3.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonResponseDto {

    private String sessionUid;
    private Integer carIndexA;
    private Integer carIndexB;

    private List<LapResponseDto> lapsA;
    private List<LapResponseDto> lapsB;
    private SessionSummaryDto summaryA;
    private SessionSummaryDto summaryB;
    private List<PacePointDto> paceA;
    private List<PacePointDto> paceB;

    /** Lap numbers actually used for traceA and speedTraceA (default: best lap from summaryA). */
    private Integer referenceLapNumA;
    private Integer referenceLapNumB;

    private List<TracePointDto> traceA;
    private List<TracePointDto> traceB;
    private List<SpeedTracePointDto> speedTraceA;
    private List<SpeedTracePointDto> speedTraceB;
}
