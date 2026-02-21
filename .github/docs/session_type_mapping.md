# F1 UDP Session Type (m_sessionType) Mapping

Single source of truth for `m_sessionType` (uint8) from **PacketSessionData** and **WeatherForecastSample**. Used by ingest (SessionDataPacketParser, SessionPacketParser) and processing (SessionMapper) so both stay in sync.

## References

- **F1 25:** `.github/docs/F1 25 Telemetry Output Structures.txt` — `PacketSessionData.m_sessionType`: *"0 = unknown, see appendix"* (appendix not in repo).
- **F1 23 (community):** [hotlaps/f1-game-udp-specs](https://github.com/hotlaps/f1-game-udp-specs) — `PacketSessionData.h`: 0–13 listed (1–4 = P1–P3/Short P, 5–9 = Q1–Q3/Short Q/OSQ, 10–12 = R/R2/R3, 13 = Time Trial). F1 23 has no Sprint.
- **F1 24/25:** Sprint weekend added; appendix in official PDF lists session types. We map 13 = Sprint (race), 14 = Sprint Shootout (qualifying) per common F1 24/25 usage.

## Canonical mapping (this codebase)

| Id (uint8) | Display string        | Notes |
|------------|------------------------|-------|
| 0          | UNKNOWN                | Unknown / invalid |
| 1          | PRACTICE_1             | P1 |
| 2          | PRACTICE_2             | P2 |
| 3          | PRACTICE_3             | P3 |
| 4          | SHORT_PRACTICE         | Short practice |
| 5          | QUALIFYING_1           | Q1 |
| 6          | QUALIFYING_2           | Q2 |
| 7          | QUALIFYING_3           | Q3 |
| 8          | SHORT_QUALIFYING       | Short qualifying |
| 9          | ONE_SHOT_QUALIFYING    | One-shot qualifying |
| 10         | RACE                   | Main race |
| 11         | RACE_2                 | Second race (e.g. some formats) |
| 12         | TIME_TRIAL             | Time Trial (F1 23 spec: R3 in some docs; we use TIME_TRIAL for 12) |
| 13         | SPRINT                 | F1 24/25: Sprint (race). F1 23: Time Trial in community spec — we prioritise F1 24/25. |
| 14         | SPRINT_SHOOTOUT        | F1 24/25: Sprint Shootout (qualifying) |
| 15         | SPRINT                 | Fallback if game uses 15 for Sprint |
| 16         | SPRINT_SHOOTOUT        | Fallback if game uses 16 for Sprint Shootout |
| other      | UNKNOWN                | Any other value (e.g. 17–255) |

## Where this is used

- **udp-ingest-service:** `SessionPacketParser.parseSessionType(byte)` (event path), `SessionDataPacketParser` (full PacketSessionData → `SessionDataDto.sessionType` as integer; display string comes from processing).
- **telemetry-processing-api-service:** `SessionMapper.sessionTypeToDisplayString(Short)` (entity → REST DTO).

Both must use the same mapping. When adding or changing a row, update this doc first, then `SessionMapper.sessionTypeToDisplayString` and `SessionPacketParser.parseSessionType`.
