# Implementation plan: Session type and track alignment with F1 25 official spec

**Topic:** Align `m_sessionType` mapping and (optionally) `m_trackId` display with the official F1 25 UDP specification.  
**Sources:** Deep research report ([type-name-track-deep-research-report.md](../type-name-track-deep-research-report.md)), official F1 25 docs in repo.  
**Status:** Draft.

---

## 1. Official documentation references

| Document | Location | Content |
|----------|----------|---------|
| **F1 25 Telemetry Output Structures** | [`.github/docs/F1 25 Telemetry Output Structures.txt`](../../docs/F1%2025%20Telemetry%20Output%20Structures.txt) | C++ structs: `PacketSessionData.m_sessionType` (uint8, "0 = unknown, see appendix"), `m_trackId` (int8, "-1 for unknown, see appendix"). No appendix in this file. |
| **Data Output from F1 25 (PDF)** | `.github/docs/Data Output from F1 25 v3.pdf` (if present in repo) or official EA/Codemasters source | Full **Session types** and **Track IDs** appendices. The deep research report extracted the tables from this appendix. |
| **Deep research report** | [`.github/draft/type-name-track-deep-research-report.md`](../type-name-track-deep-research-report.md) | Tables for F1 25: Session types (codes 0–18), Track IDs (including -1, 0, 2–7, 9–17, 19–20, 26–27, 29–32, 39–41). Offsets: `m_sessionType` at 35, `m_trackId` at 36 after header. |

**Canonical source for code:** The F1 25 appendix (PDF) is the authority; the research report reproduces it. The in-repo `.txt` file does not include the appendix, so we rely on the research report (and PDF when available) for the exact code lists.

---

## 2. Session type (`m_sessionType`) discrepancy

### 2.1 Official F1 25 mapping (from research report / PDF appendix)

| Code (uint8) | Official name |
|--------------|----------------|
| 0 | Unknown |
| 1 | Practice 1 |
| 2 | Practice 2 |
| 3 | Practice 3 |
| 4 | Short Practice |
| 5 | Qualifying 1 |
| 6 | Qualifying 2 |
| 7 | Qualifying 3 |
| 8 | Short Qualifying |
| 9 | One-Shot Qualifying |
| 10 | **Sprint Shootout 1** |
| 11 | **Sprint Shootout 2** |
| 12 | **Sprint Shootout 3** |
| 13 | **Short Sprint Shootout** |
| 14 | **One-Shot Sprint Shootout** |
| 15 | **Race** |
| 16 | **Race 2** |
| 17 | **Race 3** |
| 18 | **Time Trial** |

### 2.2 Current codebase mapping (wrong for F1 25)

Current mapping follows **F1 23** numbering (Race = 10, Time Trial = 13) with ad‑hoc F1 24/25 guesses (13 = Sprint, 14 = Sprint Shootout, 15/16 fallbacks). That contradicts the F1 25 appendix.

| Code | Current display string | Official F1 25 |
|------|------------------------|----------------|
| 10 | RACE | Sprint Shootout 1 |
| 11 | RACE_2 | Sprint Shootout 2 |
| 12 | TIME_TRIAL | Sprint Shootout 3 |
| 13 | SPRINT | Short Sprint Shootout |
| 14 | SPRINT_SHOOTOUT | One-Shot Sprint Shootout |
| 15 | SPRINT | **Race** |
| 16 | SPRINT_SHOOTOUT | **Race 2** |
| 17 | — (UNKNOWN) | **Race 3** |
| 18 | — (UNKNOWN) | **Time Trial** |

**Conclusion:** Session type mapping must be updated to match the F1 25 appendix (codes 10–18 and display strings). Codes 0–9 are already correct.

---

## 3. Track ID (`m_trackId`) — optional follow-up

- **Spec:** `m_trackId` is **int8**; **-1** = unknown. List is non-contiguous (e.g. 0, 2, 3, 4, 5, 6, 7, 9, 10, …, 26, 27, 29, 30, 31, 32, 39, 40, 41). See research report for full table.
- **Current code:**  
  - Ingest: `SessionDataPacketParser` correctly reads `trackId` as signed byte; `SessionPacketParser` uses `Byte.toUnsignedInt(buffer.get())`, so -1 becomes 255 (consider preserving -1 for “unknown” in API if needed).  
  - UI: `ui/src/constants/tracks.ts` uses a different, contiguous-ish mapping (e.g. 1 = Melbourne, 2 = Paul Ricard) that does not match F1 25 Track IDs (e.g. 0 = Melbourne, 2 = Shanghai).
- **Recommendation:** Treat track ID alignment as a **separate task**: update `tracks.ts` to the F1 25 Track ID table from the research report (and optionally handle -1/255 for unknown in ingest and API). Not in scope for the session-type-only fix below.

---

## 4. Implementation plan: session type only

### 4.1 Scope

- Update **session type** mapping to F1 25 (codes 0–18) in one place (doc) and two code paths (ingest + processing).
- No DB migration (session type remains stored as smallint/code).
- No API contract change (still string display name for session type).

### 4.2 Steps

1. **Update canonical doc**  
   - File: [`.github/docs/session_type_mapping.md`](../../docs/session_type_mapping.md).  
   - Replace the “Canonical mapping” table with the **F1 25** table (0–18) and add a reference: “Source: F1 25 official spec (Data Output from F1 25), appendix Session types; see `.github/draft/type-name-track-deep-research-report.md`.”  
   - Remove F1 23–style rows (10 = RACE, 11 = RACE_2, 12 = TIME_TRIAL, 13/14/15/16 as Sprint/Sprint Shootout/fallbacks).  
   - Add display strings for 10–18: e.g. `SPRINT_SHOOTOUT_1`, `SPRINT_SHOOTOUT_2`, `SPRINT_SHOOTOUT_3`, `SHORT_SPRINT_SHOOTOUT`, `ONE_SHOT_SPRINT_SHOOTOUT`, `RACE`, `RACE_2`, `RACE_3`, `TIME_TRIAL`.  
   - Note that F1 23 used different codes (e.g. Race = 10); we target F1 25 only for this codebase.

2. **Update ingest: SessionPacketParser**  
   - File: `udp-ingest-service/src/main/java/.../ingest/parser/SessionPacketParser.java`.  
   - Method: `parseSessionType(byte)`.  
   - Replace the switch so that 10→SPRINT_SHOOTOUT_1, 11→SPRINT_SHOOTOUT_2, 12→SPRINT_SHOOTOUT_3, 13→SHORT_SPRINT_SHOOTOUT, 14→ONE_SHOT_SPRINT_SHOOTOUT, 15→RACE, 16→RACE_2, 17→RACE_3, 18→TIME_TRIAL; default UNKNOWN.  
   - Javadoc: point to `.github/docs/session_type_mapping.md` and F1 25 appendix.

3. **Update processing: SessionMapper**  
   - File: `telemetry-processing-api-service/src/main/java/.../mapper/SessionMapper.java`.  
   - Method: `sessionTypeToDisplayString(Short)`.  
   - Apply the same mapping as in SessionPacketParser (10–18 and default).  
   - Javadoc: same references.

4. **Tests and test data**  
   - **TestData (processing):** `SESSION_TYPE_RACE` is currently `10`. In F1 25, main race is `15`. Change to `public static final short SESSION_TYPE_RACE = 15;` and ensure all usages still expect display string `"RACE"`.  
   - **SessionMapperTest:**  
     - Update `sessionTypeToDisplayString_mapsKnownTypes`: 10→SPRINT_SHOOTOUT_1, 11→SPRINT_SHOOTOUT_2, 12→SPRINT_SHOOTOUT_3, 13→SHORT_SPRINT_SHOOTOUT, 14→ONE_SHOT_SPRINT_SHOOTOUT, 15→RACE, 16→RACE_2, 17→RACE_3, 18→TIME_TRIAL; keep 0–9 and UNKNOWN for 99/-1.  
     - If any test asserts “RACE” for session type, use id **15** (e.g. in toDto or session type assertions).  
   - **SessionDataPacketParserTest (ingest):** If it asserts sessionType 10 = RACE, change to sessionType **15** for RACE, or add a test that 15 → RACE (in ingest we only pass through integer; display string is from processing). So: either keep building a buffer with sessionType=15 for “main race” in tests, or add an explicit test that parser returns 15 for the RACE case when buffer has 15.  
   - **SessionPacketHandlerTest / SessionQueryServiceTest:** Any assertion that session type string is "RACE" should use session type id **15** in the underlying data (TestData or stub).

5. **Optional: SessionEventDto Javadoc**  
   - File: `telemetry-api-contracts/.../kafka/SessionEventDto.java`.  
   - Current comment says “F1 game session type id (0–12)”. Update to “(0–18 for F1 25, see appendix)”.

### 4.3 Files to touch (summary)

| File | Change |
|------|--------|
| `.github/docs/session_type_mapping.md` | Replace table with F1 25 (0–18), add official refs. |
| `udp-ingest-service/.../parser/SessionPacketParser.java` | `parseSessionType`: 10–18 + default. |
| `telemetry-processing-api-service/.../mapper/SessionMapper.java` | `sessionTypeToDisplayString`: same mapping. |
| `telemetry-processing-api-service/.../TestData.java` | `SESSION_TYPE_RACE = 15`. |
| `telemetry-processing-api-service/.../mapper/SessionMapperTest.java` | Update session type cases and RACE to id 15. |
| `udp-ingest-service/.../parser/SessionDataPacketParserTest.java` | Use sessionType 15 for “race” if needed; align expectations. |
| `telemetry-api-contracts/.../SessionEventDto.java` | Javadoc 0–18 (optional). |

### 4.4 Verification

- Run: `mvn -pl udp-ingest-service test` and `mvn -pl telemetry-processing-api-service verify`.  
- Manually: start a session with F1 25 (Race) and confirm session type shows **RACE** (code 15).  
- Session list and session detail UI should show RACE for code 15; Sprint Shootout 1 for 10, etc.

---

## 5. References (short)

- **Session types F1 25:** [type-name-track-deep-research-report.md](../type-name-track-deep-research-report.md) § “Коди m_sessionType (F1 25)”.  
- **Track IDs F1 25:** same report § “Коди m_trackId (F1 25)”.  
- **Struct layout:** `.github/docs/F1 25 Telemetry Output Structures.txt` — `PacketSessionData`, `WeatherForecastSample.m_sessionType`.  
- **F1 23 vs F1 25:** Research report § “Порівняння з форматом F1 23” and “Відмінності між форматами F1 23 і F1 25”.
