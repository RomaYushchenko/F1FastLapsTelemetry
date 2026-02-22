# Single place for session type and track display (Enums + one place)

**Goal:** All "game code → human-readable display" **data and mapping** live in **one place** on the backend (Enums in telemetry-api-contracts). Only this place knows the codes and display names. The frontend receives **only** ready display data from the API (no raw codes for UI).

**Status:** Proposal.

---

## 1. Principle: one place

- **One place** = one module, one set of types: **telemetry-api-contracts**.
- **Storage of values + mapping by code** = Java **Enums**: `F1SessionType` (code → display name), `F1Track` (id → display name).
- **Transformation** from raw id to display string = only inside these enums (`fromCode(int)` / `fromId(Integer)` + `getDisplayName()`). No duplicate switch or map anywhere else.
- **What goes to the frontend** = only understandable data: `sessionType` (String), `trackDisplayName` (String). Filled **only** by code that uses these enums (SessionMapper in processing). Ingest sends only raw ids (Integer); it does not produce display strings.

---

## 2. Design: Enums in telemetry-api-contracts

### 2.1 F1SessionType (F1 25, m_sessionType)

- **Package:** `com.ua.yushchenko.f1.fastlaps.telemetry.api.reference`
- **Enum:** each constant has `code` (int) and `displayName` (String).
- **Mapping by code:** `F1SessionType fromCode(int code)` — returns the enum for that code, or `UNKNOWN` for unknown/out-of-range.
- **Display:** `String getDisplayName()`.

Example (F1 25, 0–18):

```text
UNKNOWN(0, "Unknown"),
PRACTICE_1(1, "Practice 1"),
PRACTICE_2(2, "Practice 2"),
PRACTICE_3(3, "Practice 3"),
SHORT_PRACTICE(4, "Short Practice"),
QUALIFYING_1(5, "Qualifying 1"),
QUALIFYING_2(6, "Qualifying 2"),
QUALIFYING_3(7, "Qualifying 3"),
SHORT_QUALIFYING(8, "Short Qualifying"),
ONE_SHOT_QUALIFYING(9, "One-Shot Qualifying"),
SPRINT_SHOOTOUT_1(10, "Sprint Shootout 1"),
SPRINT_SHOOTOUT_2(11, "Sprint Shootout 2"),
SPRINT_SHOOTOUT_3(12, "Sprint Shootout 3"),
SHORT_SPRINT_SHOOTOUT(13, "Short Sprint Shootout"),
ONE_SHOT_SPRINT_SHOOTOUT(14, "One-Shot Sprint Shootout"),
RACE(15, "Race"),
RACE_2(16, "Race 2"),
RACE_3(17, "Race 3"),
TIME_TRIAL(18, "Time Trial");
```

- For API/frontend you can either use human-readable names (`"Race"`, `"Practice 1"`) or current-style labels (`"RACE"`, `"PRACTICE_1"`) as `displayName`; choose one and keep it consistent. The enum is the single place that defines this.

### 2.2 F1Track (F1 25, m_trackId)

- **Same package.** Enum with **id** (int) and **displayName** (String). F1 25 track list is non-contiguous (-1, 0, 2, 3, … 32, 39, 40, 41).
- **Mapping:** `F1Track fromId(Integer id)` — null and -1 (unknown) → return a constant `UNKNOWN(-1, "Unknown")` or similar.
- **Display:** `String getDisplayName()`.

Example (subset):

```text
UNKNOWN(-1, "Unknown"),
MELBOURNE(0, "Melbourne"),
SHANGHAI(2, "Shanghai"),
SAKHIR(3, "Sakhir (Bahrain)"),
...
ZANDVOORT(26, "Zandvoort"),
IMOLA(27, "Imola"),
JEDDAH(29, "Jeddah"),
MIAMI(30, "Miami"),
LAS_VEGAS(31, "Las Vegas"),
LOSAIL(32, "Losail"),
SILVERSTONE_REVERSE(39, "Silverstone (Reverse)"),
AUSTRIA_REVERSE(40, "Austria (Reverse)"),
ZANDVOORT_REVERSE(41, "Zandvoort (Reverse)");
```

Full list: see [type-name-track-deep-research-report.md](../type-name-track-deep-research-report.md) § "Коди m_trackId (F1 25)".

### 2.3 Single place

- **All** session type and track display data = only in **telemetry-api-contracts**, in these two enums.
- **All** mapping from code/id to display string = only inside these enums (`fromCode`/`fromId` + `getDisplayName()`).
- No other module defines session type or track names. Ingest and processing **depend on contracts** and call only these enums.

---

## 3. Where transformation happens (still one place)

- **Producing display strings for the API** = the only place that turns entity ids into DTO fields is **SessionMapper** in **telemetry-processing-api-service**, and it **only** calls:
  - `F1SessionType.fromCode(session.getSessionType()).getDisplayName()` → `SessionDto.sessionType`
  - `F1Track.fromId(session.getTrackId()).getDisplayName()` → `SessionDto.trackDisplayName`
- So the **logic** of "id → display" lives only in the enums (contracts); the **usage** for "what the frontend sees" is only in SessionMapper. No second implementation of the mapping anywhere.

---

## 4. Ingest: only pass codes

- **udp-ingest-service** does **not** convert to display strings. It sends only:
  - `sessionTypeId` (Integer)
  - `trackId` (Integer)
- Kafka **SessionEventDto**: keep `sessionTypeId` and `trackId`; **remove** or deprecate `sessionType` (String). If logs need a name, ingest can call `F1SessionType.fromCode(id).getDisplayName()` from contracts (same single place, no new mapping).
- **SessionPacketParser**: remove local `parseSessionType(byte)` switch; pass only id. Optionally use `F1SessionType.fromCode(Byte.toUnsignedInt(b))` for logging only.

---

## 5. REST and frontend: only understandable data

- **SessionDto** (contracts):
  - `sessionType` (String) — display string from `F1SessionType.fromCode(...).getDisplayName()`.
  - `trackDisplayName` (String) — display string from `F1Track.fromId(...).getDisplayName()`.
  - Optionally keep `trackId` (Integer) for debugging or filtering; UI should use `trackDisplayName` for display.
- **Frontend:** uses only `session.sessionType` and `session.trackDisplayName`. No local map of codes or track ids; remove or minimise `ui/src/constants/tracks.ts` once API always returns `trackDisplayName`.

---

## 6. Implementation steps (summary)

| Step | Where | What |
|------|--------|------|
| 1 | **telemetry-api-contracts** | Add package `api.reference`. Add enum **F1SessionType** (code 0–18, F1 25), with `fromCode(int)` and `getDisplayName()`. Add enum **F1Track** (F1 25 track ids), with `fromId(Integer)` and `getDisplayName()`. |
| 2 | **telemetry-api-contracts** | In **SessionDto** add `trackDisplayName` (String, optional). Keep `sessionType` as String (display). |
| 3 | **telemetry-processing-api-service** | **SessionMapper.toDto()**: remove local switch. Use `F1SessionType.fromCode(session.getSessionType()).getDisplayName()` for `sessionType`, `F1Track.fromId(session.getTrackId()).getDisplayName()` for `trackDisplayName`. |
| 4 | **udp-ingest-service** | **SessionPacketParser**: stop building session type string; set only `sessionTypeId` in DTO. Remove or deprecate `sessionType` in **SessionEventDto**. Optionally use `F1SessionType.fromCode(id)` for logs. |
| 5 | **Kafka** | **SessionEventDto**: document that `sessionType` (String) is deprecated or removed; consumers use `sessionTypeId` and resolve via `F1SessionType.fromCode(sessionTypeId)` if they need a display string. Processing already has entity with id and resolves in SessionMapper. |
| 6 | **Frontend** | Add `trackDisplayName` to session type; show `session.trackDisplayName ?? session.trackId`; later drop local track map when API is stable. |
| 7 | **Docs** | `.github/docs/session_type_mapping.md`: state that canonical source is **F1SessionType** enum in telemetry-api-contracts. Plan 06 (F1 25 alignment) is implemented inside these enums. |

---

## 7. Benefits

- **One place:** All session type and track display data and all mapping by code = **only** in **telemetry-api-contracts** (F1SessionType, F1Track). Enums = storage + mapping.
- **No duplication:** No switch or map in ingest or processing; they only call enum `fromCode`/`fromId` and `getDisplayName()`.
- **Frontend** gets only understandable data: `sessionType` and `trackDisplayName` strings; no need to hold codes or track tables on the client.
- **F1 25 alignment** (plan 06) is done once inside these enums; all consumers stay in sync.
