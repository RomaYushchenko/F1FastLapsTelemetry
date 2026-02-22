# DRS / ERS — Fix Display and Use Enums

**Goal:** Fix the current **DRS and ERS** behaviour so that (1) **DRS** shows the **actual wing state** (open/closed) from Car Telemetry, and "DRS allowed" from Car Status is used only where relevant; (2) **ERS** remains from Car Status with correct interpretation using **enums**; (3) optional: show **DRS disabled reason** when we consume Event packet (DRSD).

**Status:** Proposal.

**Reference:** `.github/docs/F1 25 Telemetry Output Structures.txt` (CarTelemetryData.m_drs, CarStatusData.m_drsAllowed, m_ersStoreEnergy, m_ersDeployMode); `.github/draft/other-enume-types-deep-research-report.md` (DRS Disabled Reason in Event packet).

---

## 1. Problem Statement

- **DRS:** Today the Live snapshot sets `snapshot.setDrs(Boolean.TRUE.equals(status.getDrsAllowed()))` in **CarStatusProcessor** only. So the UI shows **"DRS allowed"** (zone active) instead of **"DRS open"** (wing actually open). The **Car Telemetry** packet carries `m_drs` (0=off, 1=on) — the real wing state — but **CarTelemetryProcessor** does not update `snapshot.setDrs()`. Result: wrong or confusing Live DRS indicator.
- **ERS:** We use `ersStoreEnergy` (converted to 0–100%) and `ersDeployMode > 0` for "deploy active". Possible issues: ERS_MAX_ENERGY_J constant; or displaying raw code instead of enum label (Medium/Hotlap/Overtake). No enum used today.
- **DRS disabled:** When DRS is turned off (e.g. rain, safety car), the game sends **Packet Event** "DRSD" with a reason (Wet track, Safety car, Red flag, Min lap not reached). We do not read Event packet yet; after **plan 08**, we can show "DRS disabled: Wet track" on Live or Session.

---

## 2. Intended Behaviour

| Data point | Source packet | Field | Use |
|------------|----------------|-------|-----|
| **DRS open (wing)** | Car Telemetry (6) | m_drs 0/1 | Live: "DRS ON/OFF" — actual wing state. |
| **DRS allowed (zone)** | Car Status (7) | m_drsAllowed 0/1 | Optional: "DRS available" or badge when in zone; or only show when DRS is ON that it was allowed. |
| **ERS energy %** | Car Status (7) | m_ersStoreEnergy | Live: 0–100% (ersStoreEnergy / ERS_MAX_ENERGY_J). |
| **ERS deploy active** | Car Status (7) | m_ersDeployMode | Live: true when ersDeployMode != 0; use **ErsDeployMode** enum for display (None/Medium/Hotlap/Overtake). |
| **DRS disabled reason** | Event (3) DRSD | reason | Optional: after plan 08, show on Live or in session context. |

---

## 3. Design

### 3.1 DRS: Use Car Telemetry for "DRS open"

- **CarTelemetryProcessor:** When updating the snapshot, set **DRS from telemetry**:  
  `snapshot.setDrs(telemetry.getDrs() != null && telemetry.getDrs() == 1);`  
  (or use `DrsState.fromCode(telemetry.getDrs()) == DrsState.ON` after plan 11.)
- **CarStatusProcessor:** **Stop** setting `snapshot.setDrs(status.getDrsAllowed())`. Optionally set a **separate** snapshot field `drsAllowed` (Boolean) if the UI should show "in DRS zone" and "wing open" separately.
- **WsSnapshotMessageBuilder / WsSnapshotMessage:**  
  - `drs` (Boolean) = wing open (from telemetry).  
  - Optional: `drsAllowed` (Boolean) = zone active (from status).  
- **UI (Live):** Primary indicator = DRS open (telemetry). Optional secondary: "DRS available" when drsAllowed true and drs false.

### 3.2 ERS: Keep Source, Add Enum

- **CarStatusProcessor:** Keep ERS energy % and deploy active from Car Status. For "deploy active", use **ErsDeployMode** (plan 11):  
  `snapshot.setErsDeployActive(ErsDeployMode.fromCode(status.getErsDeployMode()) != ErsDeployMode.NONE);`  
- Optional: add to snapshot and WS message `ersDeployModeDisplayName` (String) from enum (e.g. "Hotlap", "Overtake") for richer UI.
- Verify **ERS_MAX_ENERGY_J** (4 MJ) against F1 25 spec; document source.

### 3.3 DRS Disabled Reason (after Plan 08)

- When **EventConsumer** and **EventProcessor** handle "DRSD", persist or push **DrsDisabledReason** (enum) to session state or Live context.
- Live or Session Detail can show: "DRS disabled: Wet track" using enum display name.

### 3.4 Enums (from plan 11 and 08)

- **DrsState** — 0=off, 1=on (telemetry).
- **ErsDeployMode** — 0=none, 1=medium, 2=hotlap, 3=overtake.
- **DrsDisabledReason** — 0=Wet track, 1=Safety car, 2=Red flag, 3=Min lap not reached (Event packet, plan 08).

---

## 4. Implementation Checklist

| # | Task | Location / notes |
|---|------|------------------|
| 1 | CarTelemetryProcessor: set snapshot DRS from telemetry.drs (0/1) | telemetry-processing-api-service/.../processor/CarTelemetryProcessor.java |
| 2 | CarStatusProcessor: remove snapshot.setDrs(drsAllowed); optionally set snapshot.drsAllowed | CarStatusProcessor.java, SessionRuntimeState.CarSnapshot |
| 3 | SessionRuntimeState.CarSnapshot: optional drsAllowed (Boolean) | SessionRuntimeState.java |
| 4 | WsSnapshotMessage: drs = wing open; optional drsAllowed | telemetry-api-contracts, WsSnapshotMessageBuilder |
| 5 | Use ErsDeployMode in CarStatusProcessor for ersDeployActive and optional display name | CarStatusProcessor.java (depends on plan 11 enums) |
| 6 | Document ERS_MAX_ENERGY_J and verify value | CarStatusProcessor.java Javadoc |
| 7 | Optional: EventProcessor (plan 08) — on DRSD, set DRS disabled reason in state; expose on Live/Session | After plan 08 |
| 8 | UI (Live): show DRS from telemetry; optionally "DRS available" from status | ui/ (e.g. Live page) |
| 9 | **Documentation** | Update project docs (see § 5). |

---

## 5. Documentation updates

Keep project documentation up to date when implementing this plan.

| Document | What to update |
|----------|----------------|
| **.github/project/documentation_index.md** | Ensure Live / WebSocket / DRS-ERS behaviour is described: DRS = wing state from Car Telemetry; optional drsAllowed from Car Status; ERS from Car Status; reference plan 12. |
| **.github/project/react_spa_ui_architecture.md** | Update § Live dashboard: DRS indicator = "DRS open" (from telemetry), not "DRS allowed"; optional "DRS available" badge from status; ERS % and deploy active from Car Status; document ERS_MAX_ENERGY_J source (F1 regs / spec) if mentioned. |
| **telemetry-processing-api-service** (Javadoc) | CarStatusProcessor: document that snapshot DRS is **not** set here (set in CarTelemetryProcessor from telemetry.drs); ERS_MAX_ENERGY_J source (e.g. F1 25 spec, 4 MJ). |

---

## 6. Dependencies

- **Plan 11 (Car Telemetry/Status/Damage enums):** DrsState, ErsDeployMode.
- **Plan 08 (Packet Event):** DRS disabled reason (DRSD event) and DrsDisabledReason enum.

---

## 7. Testing

- Unit test: CarTelemetryProcessor with drs=1 → snapshot.drs true; with drs=0 → snapshot.drs false.
- Unit test: CarStatusProcessor no longer sets snapshot.drs from drsAllowed; snapshot.drsAllowed (if added) set from status.
- Integration: Live WebSocket message contains drs (wing) and optionally drsAllowed; ERS % and ersDeployActive unchanged in behaviour, with enum used internally.

---

## 8. Summary

| Issue | Fix |
|-------|-----|
| DRS shows "allowed" not "open" | Use Car Telemetry `drs` for snapshot.drs; stop using drsAllowed for snapshot.drs. |
| ERS deploy: raw code | Use ErsDeployMode enum for logic and optional display. |
| DRS disabled reason | After plan 08, consume DRSD event and expose DrsDisabledReason. |
