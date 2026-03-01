# New UI & Backend — implementation block plans

This folder contains **per-block implementation plans** for the [Step-by-Step Implementation Plan — New UI & Backend](../../../IMPLEMENTATION_STEP_BY_STEP.md). Each file holds the steps, detailed changes, testing, and documentation updates for one block.

| Block | File | Steps |
|-------|------|--------|
| A — Foundation and session list/detail | [block-a-foundation-and-session-list-detail.md](block-a-foundation-and-session-list-detail.md) | 1–6 |
| B — Session list filters | [block-b-session-list-filters.md](block-b-session-list-filters.md) | 7–8 |
| C — Live (WebSocket) | [block-c-live-websocket.md](block-c-live-websocket.md) | 9–13 |
| C follow-up — Live snapshot tyre/fuel | [block-c-follow-up-live-snapshot-tyre-fuel.md](block-c-follow-up-live-snapshot-tyre-fuel.md) | — (after Block C) |
| D — Strategy View (pit-stops, stints) | [block-d-strategy-view.md](block-d-strategy-view.md) | 14–16 |
| E — Live leaderboard and events | [block-e-live-leaderboard-and-events.md](block-e-live-leaderboard-and-events.md) | 17–20 |
| F — Live Track Map | [block-f-live-track-map.md](block-f-live-track-map.md) | 21–22 |
| G — Driver Comparison (optional) | [block-g-driver-comparison.md](block-g-driver-comparison.md) | 23–24 |
| H — Optional (fuel, ERS, positions, auth) | [block-h-optional-fuel-ers-positions-auth.md](block-h-optional-fuel-ers-positions-auth.md) | 25–30 |
| I — Supplementary UI actions | [block-i-supplementary-ui-actions.md](block-i-supplementary-ui-actions.md) | 31–35 |
| J — Verification, old UI removal, docs | [block-j-verification-old-ui-removal-docs.md](block-j-verification-old-ui-removal-docs.md) | 36–40 |

**Gap analysis:** [MISSING_UI_ACTIONS_ANALYSIS.md](MISSING_UI_ACTIONS_ANALYSIS.md) lists buttons and elements in the new UI that were not fully covered in the original plan; Block I and updates to blocks B and C address them.

Use the main plan ([IMPLEMENTATION_STEP_BY_STEP.md](../../../IMPLEMENTATION_STEP_BY_STEP.md)) for the overall order, dependency overview, summary table, and test/documentation checklist; use these files for block-level detail.

---

## Frontend component reusability (all blocks)

When implementing UI in **f1-telemetry-web-platform**, avoid code duplication by reusing existing components and by extracting new shared components when needed.

### Use existing components

- **App-level components** (`src/app/components/`): use **DataCard** (cards with title, variant, actions), **StatusBadge** (Live/Waiting/Error/Disconnected etc.), **TelemetryStat** (label + value + unit, variants), **AppLayout**, **ImageWithFallback** where they fit. Do not reimplement equivalent card/badge/stat UI inline.
- **Shadcn UI** (`src/app/components/ui/`): use **Dialog**, **DropdownMenu**, **Popover**, **Select**, **Table**, **Button**, **Card**, **AlertDialog**, **Tooltip**, **Skeleton**, **Badge**, etc. from this folder. Do not add duplicate UI primitives; import from `@/app/components/ui/...` (or the project’s alias).

### Before adding a new component

1. Check **components/** and **components/ui/** for an existing component that can be reused or extended (e.g. another variant or prop).
2. If the same pattern appears in multiple pages (e.g. “No active session” message, error + Retry block, loading skeleton for a table), **extract a shared component** (e.g. `EmptyState`, `ErrorWithRetry`, `PageSkeleton`) and reuse it everywhere instead of copying markup.
3. New shared components belong in **components/** (domain or layout) or **components/ui/** (generic primitives). Document them briefly so future blocks reuse them.

### Shared patterns to reuse or unify

- **Loading:** Skeleton or spinner — use the same pattern (e.g. Skeleton from ui) across Session History, Session Details, Live Overview, Strategy View, Driver Comparison, etc.
- **Error + Retry:** One pattern (message + Retry button + optional toast); reuse the same component or the same structure so behaviour and copy stay consistent.
- **Empty state:** “No sessions”, “No pit stops”, “No active session” — prefer one shared `EmptyState` (or small set) with configurable message and optional action/link.
- **Tables:** Use the same Table (from ui) and column patterns; avoid one-off table markup that duplicates structure.
- **Format helpers:** Use shared `formatLapTime`, `getTrackName`, compound code → label map in one place (e.g. `api/format.ts`, `constants/tracks.ts`); do not duplicate formatters per page.

Each block plan that includes UI work should follow this guideline; when in doubt, prefer reusing or extending an existing component over adding a new duplicate.
