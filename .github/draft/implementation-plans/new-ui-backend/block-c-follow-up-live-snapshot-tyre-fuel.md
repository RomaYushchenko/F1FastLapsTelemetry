# Block C follow-up — Live snapshot: tyre temperatures and fuel

**Do not forget:** This is a follow-up to [block-c-live-websocket.md](block-c-live-websocket.md). In Block C, tyre temperatures and fuel in the Live UI are shown as **mock** (or "N/A") because the WebSocket snapshot contract does not yet include them. This document ensures we implement real data later.

**Reference:** [rest_web_socket_api_contracts_f_1_telemetry.md](../../project/rest_web_socket_api_contracts_f_1_telemetry.md) §4.5.1 (Live Snapshot). Backend: `WsSnapshotMessage`, `WsSnapshotMessageBuilder`, `SessionRuntimeState.CarSnapshot`; F1 25 CarTelemetry has tyre temps, CarStatus has fuel.

---

## Goal

- **Tyre temperatures:** Show real-time tyre temps (e.g. FL, FR, RL, RR °C) in Live Overview "Your Telemetry" and in Live Telemetry "Tyre Temperatures" widget, from WebSocket snapshot.
- **Fuel:** Show real-time fuel level (e.g. % or remaining) in Live Telemetry "ERS & Fuel" from WebSocket snapshot.

---

## Backend (telemetry-processing-api-service)

| # | Task | Deliverable |
|---|------|-------------|
| 1 | **Contract:** Extend WebSocket snapshot in [rest_web_socket_api_contracts_f_1_telemetry.md](../../project/rest_web_socket_api_contracts_f_1_telemetry.md) §4.5.1 with optional fields: `tyresSurfaceTempC` (array of 4: RL, RR, FL, FR) and `fuelRemainingPercent` (0–100). Source: CarTelemetryDto `tyresSurfaceTemperature`, CarStatus/fuel if available in F1 25. | Contract updated |
| 2 | **WsSnapshotMessage** (telemetry-api-contracts): Add optional `int[] tyresSurfaceTempC` (or similar), `Integer fuelRemainingPercent`. | DTO updated |
| 3 | **SessionRuntimeState.CarSnapshot** (or builder input): Ensure snapshot has tyre temps and fuel from car telemetry / car status. | State/builder has data |
| 4 | **WsSnapshotMessageBuilder:** Map tyre temps and fuel from CarSnapshot to WsSnapshotMessage. | Snapshot includes tyre + fuel |
| 5 | **LiveDataBroadcaster:** No change if builder fills the message. Verify snapshot sent to client contains new fields. | 10 Hz broadcast includes new fields |

---

## Component reusability

Follow [README § Frontend component reusability](README.md#frontend-component-reusability-all-blocks). Use **TelemetryStat** (or existing tyre/fuel widgets) in Live Overview and Live Telemetry; do not add duplicate stat or gauge markup. Reuse the same layout and card structure as in Block C.

---

## New UI (f1-telemetry-web-platform)

| # | Task | Deliverable |
|---|------|-------------|
| 6 | **ws/types.ts:** Add to WsSnapshotMessage: `tyresSurfaceTempC?: number[] \| null` (order RL, RR, FL, FR), `fuelRemainingPercent?: number \| null`. | Types updated |
| 7 | **Live Overview:** In "Your Telemetry" (or a compact tyre block), show tyre temps from `snapshot.tyresSurfaceTempC` (e.g. FL, FR, RL, RR). Show fuel from `snapshot.fuelRemainingPercent` if desired. | Live Overview shows real tyre/fuel |
| 8 | **Live Telemetry:** Replace mock "Tyre Temperatures" widget with data from `snapshot.tyresSurfaceTempC`. Replace mock fuel in "ERS & Fuel" chart/section with `snapshot.fuelRemainingPercent`. | Live Telemetry shows real tyre temps and fuel |

---

## Checklist (tracking)

- [x] B1 Contract: snapshot §4.5.1 extended with tyre temps + fuel
- [x] B2 WsSnapshotMessage (Java) extended
- [x] B3 CarSnapshot / builder input has tyre + fuel
- [x] B4 WsSnapshotMessageBuilder maps tyre + fuel
- [x] B5 Verify broadcast sends new fields (builder fills message; LiveDataBroadcaster unchanged)
- [x] U1 ws/types.ts extended
- [x] U2 Live Overview uses snapshot.tyresSurfaceTempC and fuelRemainingPercent
- [x] U3 Live Telemetry Tyre Temperatures and ERS & Fuel from snapshot
- [ ] Add git commit with understanding message
---

## When to do this

After Block C is done. Can be scheduled as a small "Block C follow-up" or folded into the next backend/UI sprint. Main point: **do not leave tyre and fuel as mock forever** — this plan is the reminder and the implementation guide.
