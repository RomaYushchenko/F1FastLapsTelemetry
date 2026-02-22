# F1 UDP Session Type (m_sessionType) Mapping

**Canonical implementation:** `F1SessionType` enum in **telemetry-api-contracts** (`api.reference.F1SessionType`). F1 25 mapping (0–18). Single place for code → display string.

## References

- **F1 25:** `.github/docs/F1 25 Telemetry Output Structures.txt` — `PacketSessionData.m_sessionType`: *"0 = unknown, see appendix"*. Full table: `.github/draft/type-name-track-deep-research-report.md`.
- **F1 23:** Different numbering (e.g. Race = 10); this codebase targets F1 25.

## Canonical mapping (F1 25, in code: F1SessionType)

| Id (uint8) | Display string        |
|------------|------------------------|
| 0          | UNKNOWN                |
| 1          | PRACTICE_1             |
| 2          | PRACTICE_2             |
| 3          | PRACTICE_3             |
| 4          | SHORT_PRACTICE         |
| 5          | QUALIFYING_1           |
| 6          | QUALIFYING_2           |
| 7          | QUALIFYING_3           |
| 8          | SHORT_QUALIFYING       |
| 9          | ONE_SHOT_QUALIFYING    |
| 10         | SPRINT_SHOOTOUT_1      |
| 11         | SPRINT_SHOOTOUT_2      |
| 12         | SPRINT_SHOOTOUT_3      |
| 13         | SHORT_SPRINT_SHOOTOUT  |
| 14         | ONE_SHOT_SPRINT_SHOOTOUT |
| 15         | RACE                   |
| 16         | RACE_2                 |
| 17         | RACE_3                 |
| 18         | TIME_TRIAL             |
| other      | UNKNOWN                |

## Where this is used

- **telemetry-api-contracts:** `F1SessionType.fromCode(int)` — single source of truth.
- **udp-ingest-service:** Sends only `sessionTypeId` (Integer). Logging can use `F1SessionType.fromCode(id).getDisplayName()`.
- **telemetry-processing-api-service:** `SessionMapper.toDto()` uses `F1SessionType.fromCode(session.getSessionType()).getDisplayName()` for REST `SessionDto.sessionType`.

When adding or changing a value, update **only** `F1SessionType` in telemetry-api-contracts; then this doc.

## Session options (weather, formula, assists, etc.)

**Session type** and **track** are above (F1SessionType, F1Track). Other PacketSessionData fields (weather, formula, safetyCarStatus, assists, sessionLength, units, game rules) are mapped via **reference enums** from **plan 09** in `telemetry-api-contracts` (package `api.reference`). Display names for REST or logs: use **SessionDataMapper** in telemetry-processing-api-service (`SessionDataMapper.weatherDisplayName(code)`, etc.). See `.github/draft/implementation-plans/09-session-packet-enums-replacement.md`.
