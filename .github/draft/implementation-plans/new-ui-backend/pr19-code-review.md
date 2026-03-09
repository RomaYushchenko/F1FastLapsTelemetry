# Code Review — PR #19: Live Track Map — Telemetry-based Track Recording

**Автор:** Claude (AI review)  
**PR:** [#19 — Live Track Map - telematry based track recording](https://github.com/RomaYushchenko/F1FastLapsTelemetry/pull/19)  
**Дата:** 2026-03-09  
**Версія плану:** v6  
**Вердикт:** 🟡 CHANGES REQUIRED — 3 критичних баги блокують рендеринг треку та позиції болідів

---

## Зміст

1. Що зроблено правильно
2. Баги (критичні → середні)
3. Аналіз plan checklist — що виконано / не виконано
4. План виправлень (Gap Fix Plan)

---

## 1. Що зроблено правильно ✅

| Область | Що реалізовано |
|---------|----------------|
| DB migration 23 | Колонки `min_elev`, `max_elev`, `source`, `recorded_at`, `session_uid`, `sector_boundaries` додані |
| Backend DTO contracts | `SectorBoundaryDto`, `TrackLayoutExportDto`, `TrackLayoutBulkExportDto`, `BulkImportResultDto` — всі нові DTOs створені |
| Multi-car motion ingest | `MotionPacketHandler` парсить усі 22 машини та публікує в Kafka ✓ |
| B9 positions backend | `MotionConsumer` → `state.updatePosition()` → `LiveDataBroadcaster` → WS POSITIONS message ✓ |
| REST positions endpoint | `GET /api/sessions/active/positions` через `LeaderboardController` ✓ |
| Positions polling frontend | `LiveTrackMap.tsx` — REST polling кожні 2 секунди через `getActivePositions()` ✓ |
| Export/Import single | `GET /{trackId}/layout/export`, `POST /layout/import` endpoints ✓ |
| Export/Import bulk | `GET /layout/export-all`, `POST /layout/import-all` endpoints ✓ |
| `TrackLayoutBoundsDto` | Оновлено: тепер має `minZ`/`maxZ` замість `minY`/`maxY` + `minElev`/`maxElev` ✓ |
| Export/Import UI кнопки | Export, Export All, Import у header LiveTrackMap ✓ |
| `source: "STATIC"` | Правильно повертається для старих треків ✓ |
| `sectorBoundaries: []` | Правильно порожній масив для STATIC треків ✓ |

---

## 2. Баги

### 🔴 BUG-01 (КРИТИЧНИЙ): SVG viewBox використовує `bounds.minY`/`bounds.maxY`, яких більше не існує

**Файл:** `f1-telemetry-web-platform/src/app/pages/LiveTrackMap.tsx`

**Що відбувається:**

```typescript
// ЩО Є ЗАРАЗ — BROKEN:
const viewBox = bounds
    ? `${bounds.minX} ${bounds.minY} ${bounds.maxX - bounds.minX} ${bounds.maxY - bounds.minY}`
    : "0 0 800 600";
// bounds тепер = { minX, maxX, minZ, maxZ, minElev, maxElev }
// bounds.minY = undefined → viewBox = "100 undefined 600 NaN"
// SVG з невалідним viewBox нічого не рендерить!
```

**Доказ зі скріншота:**
```
bounds: {minX: 100, maxX: 700, minZ: 100, maxZ: 500, minElev: null, maxElev: null}
```

`minY` і `maxY` відсутні в новому DTO. Результат — `viewBox = "100 undefined 600 NaN"` → SVG рендерить порожній canvas.

**Також зламано:**
```typescript
function boundsFromPoints(points: { x: number; y: number }[]): { ... } {
    // Fallback теж рахує minY/maxY замість minZ/maxZ
    // Для записаних треків point.y = elevation (не Z), point.z = worldZ
}
```

**Виправлення:**

```typescript
// SVG 2D uses XZ plane: worldX → svg.x, worldZ → svg.y
const viewBox = bounds
    ? `${bounds.minX} ${bounds.minZ} ${bounds.maxX - bounds.minX} ${bounds.maxZ - bounds.minZ}`
    : "0 0 800 600";

// pointsToPath — використовувати z (worldZ) як SVG Y-вісь
// Для сумісності зі STATIC треками (де z=null, y=worldZ): z ?? y
function pointsToPath(points: TrackPoint3D[]): string {
    if (points.length === 0) return "";
    const [first, ...rest] = points;
    const fy = first.z ?? first.y;  // z для recorded, y для old static
    let d = `M ${first.x} ${fy}`;
    for (const p of rest) {
        const py = p.z ?? p.y;
        d += ` L ${p.x} ${py}`;
    }
    return d;
}

// boundsFromPoints — fallback для старих треків
function boundsFromPoints(points: TrackPoint3D[]) {
    if (points.length === 0) return null;
    let minX = points[0].x, maxX = points[0].x;
    let minZ = points[0].z ?? points[0].y;  // fallback для old format
    let maxZ = points[0].z ?? points[0].y;
    for (const p of points) {
        const pz = p.z ?? p.y;
        minX = Math.min(minX, p.x);
        maxX = Math.max(maxX, p.x);
        minZ = Math.min(minZ, pz);
        maxZ = Math.max(maxZ, pz);
    }
    return { minX, maxX, minZ, maxZ };
}
```

---

### 🔴 BUG-02 (КРИТИЧНИЙ): DB migration конвертує лише Silverstone (track_id=8), але всі інші STATIC треки залишились у старому форматі `{x,y}` з `z=null`

**Файл:** `infra/init-db/23-track-layout-recording.sql`

**Що відбувається:**

```sql
-- ЗАРАЗ — ТІЛЬКИ SILVERSTONE:
UPDATE telemetry.track_layout
SET points = (SELECT jsonb_agg(jsonb_build_object('x', (el->>'x')::float, 'y', 0.0, 'z', (el->>'y')::float))
              FROM jsonb_array_elements(points) AS el),
    min_elev = 0.0, max_elev = 0.0
WHERE track_id = 8;  -- ← ТІЛЬКИ TRACK 8!
```

**Доказ зі скріншота:**
```
trackId: 3 (Sakhir/Bahrain)
points: [{x: 100, y: 300, z: null}, ...]  ← z=null для track 3!
```

Трек 3 (Sakhir) НЕ був сконвертований → `z: null` в усіх точках.

**ВАЖЛИВО:** Для BUG-01 fix (`z ?? y`) цей баг стає некритичним для 2D рендерингу (оскільки `y` містить worldZ у старому форматі), але:
- 3D рендеринг (`TrackMap3D.tsx`) НЕ буде правильно працювати
- Дані непослідовні: деякі треки мають `z`, інші — ні

**Виправлення (нова міграція `24-migrate-all-static-tracks.sql`):**

```sql
-- Конвертуємо ВСІ старі STATIC треки (де points ще в форматі {x,y} без z)
UPDATE telemetry.track_layout
SET points = (
    SELECT jsonb_agg(
        CASE
            -- Якщо вже є z поле — не чіпаємо
            WHEN (el->>'z') IS NOT NULL THEN el
            -- Якщо ні — конвертуємо: старий y = worldZ, y тепер = elevation (0.0)
            ELSE jsonb_build_object(
                'x', (el->>'x')::float,
                'y', 0.0,
                'z', (el->>'y')::float
            )
        END
    )
    FROM jsonb_array_elements(points) AS el
),
    min_elev = COALESCE(min_elev, 0.0),
    max_elev = COALESCE(max_elev, 0.0)
WHERE source = 'STATIC'
  AND points IS NOT NULL
  AND (points->0->>'z') IS NULL;  -- тільки ті, де точки ще в старому форматі
```

---

### 🔴 BUG-03 (КРИТИЧНИЙ): Car positions не відображаються на SVG карті після виправлення BUG-01

**Файл:** `f1-telemetry-web-platform/src/app/pages/LiveTrackMap.tsx`

**Що відбувається:**

Після виправлення BUG-01 positions будуть в правильному SVG coordinate space, ЯКЩО SVG рендерить маркери в абсолютних world coordinates. Але поточний код:

```typescript
const driversForMap = useRealPositions
    ? positions.map((p) => ({
        id: p.carIndex,
        x: p.worldPosX,   // world coords: наприклад 234.5
        y: p.worldPosZ,   // world coords: наприклад 891.3
      }))
    : [...mock...];
```

Це правильно тільки якщо SVG рендерить markers за абсолютними world координатами в тому ж viewBox. Потрібно переконатись що `TrackMap2D` (або вбудований SVG у `LiveTrackMap`) рендерить circle/dot:

```tsx
<circle cx={driver.x} cy={driver.y} r={8} fill={driver.color} />
```

Якщо `cx={driver.x}` використовує world coordinate → `cx=234.5`, і viewBox починається з `minX=100`, то маркер буде на відносній позиції `234.5 - 100 = 134.5` пікселів від лівого краю → **це правильно** бо SVG viewBox автоматично трансформує!

**Проблема:** Або `TrackMap2D` НЕ рендерить маркери взагалі, або рендерить у нормалізованих [0,1] координатах, а не у world coordinates.

**Потрібно перевірити в `TrackMap2D.tsx`:**
```typescript
// Варіант A (world coords, рекомендований):
<circle cx={driver.x} cy={driver.y} r={strokeWidth * 2} fill={driver.color} />

// Варіант B (normalized 0-1 — BROKEN якщо viewBox у world coords):
const nx = (driver.x - minX) / (maxX - minX) * width
// Тут normalization потрібна лише якщо viewBox="0 0 width height" (normalized)
```

**Виправлення (якщо TrackMap2D використовує нормалізацію):**

Привести до єдиного підходу: або все у world coords (viewBox = world bounds), або все нормалізоване (viewBox = "0 0 1000 1000").

**Рекомендація:** Залишити world coords (viewBox = world bounds) — простіше і вже частково реалізовано.

---

### 🟡 BUG-04 (СЕРЕДНІЙ): `TrackLayoutPointDto` у project knowledge — стара версія з `x,y` без `z`

**Файл:** `telemetry-api-contracts/...TrackLayoutPointDto.java`

**Що є в project knowledge (до PR):**
```java
public class TrackLayoutPointDto {
    private Double x;
    private Double y;   // ← нема z!
}
```

**Що має бути після PR:**
```java
public class TrackLayoutPointDto {
    private Double x;
    private Double y;   // elevation (worldY)
    private Double z;   // worldZ (horizontal)
}
```

**Доказ зі скріншота:** `points: [{x: 100, y: 300, z: null}]` — поле `z` є в API response, але в project knowledge DTO воно відсутнє. Це означає що PR додав `z`, але Jackson повертає `null` для старих треків (де в JSONB нема поля `z`).

**Вплив:** Бачимо `z: null` — це очікувано для старих треків (до конвертації BUG-02). Після виправлення BUG-02 `z` буде мати значення.

---

### 🟡 BUG-05 (СЕРЕДНІЙ): `TrackLayoutService.toDto()` — стара логіка маппінгу bounds

**Файл:** `telemetry-processing-api-service/.../TrackLayoutService.java`

**Що є в project knowledge (СТАРА версія):**
```java
private TrackLayoutResponseDto toDto(TrackLayout entity) {
    ...
    if (entity.getMinX() != null && entity.getMinY() != null && ...) {
        bounds = TrackLayoutBoundsDto.builder()
            .minX(entity.getMinX())
            .minY(entity.getMinY())   // ← стара назва поля
            .maxX(entity.getMaxX())
            .maxY(entity.getMaxY())   // ← стара назва поля
            .build();
    }
    ...
}
```

**Але API відповідь показує `minZ`/`maxZ`** — означає PR оновив цей метод. Потрібно переконатись що нова версія виглядає так:

```java
bounds = TrackLayoutBoundsDto.builder()
    .minX(entity.getMinX())
    .maxX(entity.getMaxX())
    .minZ(entity.getMinZ())     // було minY (worldZ column)
    .maxZ(entity.getMaxZ())     // було maxY (worldZ column)
    .minElev(entity.getMinElev())
    .maxElev(entity.getMaxElev())
    .build();
```

Якщо PR це вже зробив — ✓ OK. Але якщо entity поля все ще називаються `minY`/`maxY` в DB column mapping → треба перевірити що `@Column(name = "min_y")` правильно повертає worldZ значення.

---

### 🟡 BUG-06 (СЕРЕДНІЙ): `TrackLayout` entity — нові колонки не в project knowledge

**Файл:** `telemetry-processing-api-service/.../TrackLayout.java`

**Що є (стара версія):**
```java
public class TrackLayout {
    private Short trackId;
    private String pointsJson;
    private Short version;
    private Double minX;
    private Double minY;   // насправді worldZ (горизонтальна)
    private Double maxX;
    private Double maxY;   // насправді worldZ (горизонтальна)
    // ← немає: minElev, maxElev, source, recordedAt, sessionUid, sectorBoundariesJson
}
```

**Що має бути:**
```java
public class TrackLayout {
    private Short trackId;
    private String pointsJson;
    private Short version;
    private Double minX;
    @Column(name = "min_y")  // column name unchanged, still min_y
    private Double minZ;     // renamed field to clarify it's worldZ
    private Double maxX;
    @Column(name = "max_y")
    private Double maxZ;     // renamed field
    private Double minElev;
    private Double maxElev;
    private String source;
    private Instant recordedAt;
    private Long sessionUid;
    private String sectorBoundariesJson;
}
```

**Note:** Якщо колонки в БД все ще `min_y`/`max_y` (старі назви), то @Column mapping важливий. Альтернативно — залишити entity fields `minY`/`maxY` але в DTO перейменувати на `minZ`/`maxZ`.

---

## 3. Аналіз plan checklist — що виконано

### Phase 1 — Backend Core

| # | Пункт | Статус | Коментар |
|---|-------|--------|----------|
| 1.1 | DB migration `23-track-layout-recording.sql` | ✅ | Колонки є, але Silverstone-only migration |
| 1.2 | `PointXYZD` record (x, y, z, lapDistance) | ✅ | Судячи по API — є |
| 1.3 | `TrackRecordingState` з буфером | ✅ | Є (судячи по записаних треках) |
| 1.4 | `SessionRuntimeState` + sector distances | ✅ | `updatePosition()` є в коді |
| 1.5 | `TrackLayoutEntity` нові поля | ⚠️ | Є в PR, але entity mapping може бути неточним |
| 1.6 | `onMotionFrame()` з lapDistance | ✅ | `MotionConsumer` передає worldPositionY + lapDistance |
| 1.7 | `saveTrackLayout()` + `buildSectorBoundaries()` | ✅ | Судячи по `sectorBoundaries: []` у відповіді |
| 1.8 | `SessionDataConsumer` → sector distances | ✅ | |
| 1.9 | `MotionConsumer` → worldPositionY + lapDistance | ✅ | |
| 1.10 | `LapDataProcessor` → `onLapComplete()` | ✅ | |
| 1.11 | `SessionLifecycleService` → onSessionStart/Finished | ✅ | |

### Phase 2 — Backend API

| # | Пункт | Статус | Коментар |
|---|-------|--------|----------|
| 2.1 | `TrackPointDto {x,y,z}` | ✅ | `z` є у відповіді (null для старих) |
| 2.2 | `BoundsDto` з `minElev`, `maxElev` | ✅ | У відповіді є `minElev: null` |
| 2.3 | `SectorBoundaryDto {sector,x,y,z}` | ✅ | DTO є |
| 2.4 | `TrackLayoutResponseDto` + sectorBoundaries | ✅ | `sectorBoundaries: []` у відповіді |
| 2.5 | `TrackLayoutStatusDto` | ✅ | |
| 2.6 | `TrackLayoutService.getLayoutStatus()` | ✅ | `status` endpoint є |
| 2.7 | `TrackController` status endpoint | ✅ | |
| 2.8 | REST contract doc | ✅ | Оновлено |
| 2.9 | Unit tests — Recording + `buildSectorBoundaries` | ❓ | Не перевірено |
| 2.10 | MockMvc tests | ❓ | Не перевірено |

### Phase 3 — Frontend 2D

| # | Пункт | Статус | Коментар |
|---|-------|--------|----------|
| 3.1 | API types — `TrackPoint3D`, `TrackBounds`, etc. | ⚠️ | Є, але `TrackBounds` може мати `minY` замість `minZ` |
| 3.2 | `getTrackLayoutStatus(trackId)` | ✅ | |
| 3.3 | `trackNormalization.ts` | ✅ | Файл є |
| 3.4 | `TrackMap2D.tsx` з секторами | ✅ | Файл є |
| 3.5 | `LiveTelemetryProvider` positions | ⚠️ | REST polling є, WS POSITIONS — невідомо |
| 3.6 | Effect 1 — trackId change | ✅ | `useEffect([trackId])` є |
| 3.7 | Effect 2 — status polling | ⚠️ | Є `status` endpoint, але polling logic? |
| 3.8 | Recording indicator UI | ✅ | `status` відображається |
| 3.9 | No-session + NOT_AVAILABLE | ✅ | `noActiveSession` check є |
| 3.10 | Positions → TrackMap2D | ⚠️ | `driversForMap` є, але рендер в SVG? |
| **3.11** | **Manual test: сектори відображаються** | **❌ FAILED** | **Трек порожній (BUG-01)** |
| **3.12** | **Manual test: auto-reactivity** | **❌ FAILED** | **Не можна підтвердити через BUG-01** |

### Phase 4 — Frontend 3D

| # | Пункт | Статус | Коментар |
|---|-------|--------|----------|
| 4.1 | npm install @react-three/fiber | ✅ | `package.json` оновлено |
| 4.2 | 3D normalization utils | ✅ | `trackNormalization.ts` є |
| 4.3 | `TrackMap3D.tsx` | ✅ | Файл є |
| 4.4 | 3D маркери секторів | ✅ | |
| 4.5 | 2D/3D toggle | ✅ | Видно на скріншоті |
| **4.6** | **Verify elevation + sectors у 3D** | **❌ FAILED** | **Через BUG-02 (z=null) 3D не буде правильним** |
| 4.7 | `worldPosY` в CarPositionDto | ⚠️ | `CarPositionDto` має лише `worldPosX`/`worldPosZ` |

### Phase 5 — Export/Import

| # | Пункт | Статус | Коментар |
|---|-------|--------|----------|
| 5.1–5.8 | Export/Import backend endpoints | ✅ | Всі є |
| 5.9 | Unit tests для bulk | ❓ | Не перевірено |
| 5.10–5.11 | Frontend Export/Import UI | ✅ | Кнопки видно на скріншоті |
| 5.12 | Manual test E2E single | ❌ | Не можна підтвердити через BUG-01 |
| 5.13 | Manual test bulk | ❌ | Не можна підтвердити через BUG-01 |

---

## 4. Gap Fix Plan — план виправлень

### Fix 1 — ViewBox і path координати (BUG-01)

**Файл:** `f1-telemetry-web-platform/src/app/pages/LiveTrackMap.tsx`  
**Складність:** ⭐ (30 хвилин)

```typescript
// 1. viewBox — замінити minY/maxY на minZ/maxZ
const viewBox = bounds
    ? `${bounds.minX} ${bounds.minZ} ${bounds.maxX - bounds.minX} ${bounds.maxZ - bounds.minZ}`
    : "0 0 800 600";

// 2. pointsToPath — використовувати z ?? y (backward compat)
function pointsToPath(points: Array<{ x: number; y: number; z?: number | null }>): string {
    if (points.length === 0) return "";
    const [first, ...rest] = points;
    let d = `M ${first.x} ${first.z ?? first.y}`;
    for (const p of rest) {
        d += ` L ${p.x} ${p.z ?? p.y}`;
    }
    return d;
}

// 3. boundsFromPoints — рахувати z ?? y для другої осі
function boundsFromPoints(points: Array<{ x: number; y: number; z?: number | null }>) {
    if (points.length === 0) return null;
    const first = points[0];
    let minX = first.x, maxX = first.x;
    let minZ = first.z ?? first.y, maxZ = first.z ?? first.y;
    for (const p of points) {
        const pz = p.z ?? p.y;
        minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
        minZ = Math.min(minZ, pz);  maxZ = Math.max(maxZ, pz);
    }
    return { minX, maxX, minZ, maxZ };
}
```

**Також перевірити `api/types.ts`:**

```typescript
// Якщо TrackLayoutBoundsDto ще має minY/maxY — замінити:
export interface TrackLayoutBoundsDto {
    minX: number;
    maxX: number;
    minZ: number;   // ← не minY!
    maxZ: number;   // ← не maxY!
    minElev?: number | null;
    maxElev?: number | null;
}
```

---

### Fix 2 — DB Migration для всіх STATIC треків (BUG-02)

**Новий файл:** `infra/init-db/24-migrate-all-static-tracks.sql`  
**Складність:** ⭐ (15 хвилин)

```sql
-- Конвертуємо ВСІ STATIC треки зі старого формату {x,y} у новий {x,y,z}
-- де старий y = worldZ, новий y = elevation (0.0), новий z = worldZ
-- Застосовується ТІЛЬКИ до треків де points ще немає поля z
UPDATE telemetry.track_layout
SET points = (
    SELECT jsonb_agg(
        jsonb_build_object(
            'x', (el->>'x')::float,
            'y', 0.0,
            'z', (el->>'y')::float
        )
    )
    FROM jsonb_array_elements(points) AS el
),
    min_elev = COALESCE(min_elev, 0.0),
    max_elev = COALESCE(max_elev, 0.0)
WHERE (points->0->>'z') IS NULL
  AND points IS NOT NULL;
```

**Увага:** Запусти після рестарту БД. Перевір перед міграцією:
```sql
SELECT track_id, source, points->0 as first_point 
FROM telemetry.track_layout;
```

---

### Fix 3 — Перевірити і виправити рендер car positions в SVG (BUG-03)

**Файл:** `f1-telemetry-web-platform/src/app/components/TrackMap2D.tsx`  
**Складність:** ⭐⭐ (1-2 години)

**Що перевірити:**

```tsx
// TrackMap2D повинна рендерити markers у world coordinates (не normalized):
{drivers.map(d => (
    <circle
        key={d.id}
        cx={d.x}         // worldPosX — directly in world coords
        cy={d.y}         // worldPosZ — directly in world coords
        r={strokeWidth * 3}
        fill={d.color}
    />
))}
```

**Якщо зараз нормалізація до [0,1]:**

```tsx
// CURRENT (hypothetical broken):
cx={(d.x - minX) / (maxX - minX)}  // [0,1] — не підходить якщо viewBox у world coords

// FIX: залишити world coords — viewBox вже задає трансформацію
cx={d.x}
cy={d.y}
```

**Також перевірити SVG overflow:**

```tsx
<svg
    viewBox={viewBox}
    preserveAspectRatio="xMidYMid meet"
    style={{ width: '100%', height: '100%', overflow: 'visible' }}
>
```

---

### Fix 4 — `api/types.ts` — `TrackPoint3D` тип (BUG-04)

**Файл:** `f1-telemetry-web-platform/src/api/types.ts`  
**Складність:** ⭐ (10 хвилин)

```typescript
// Переконатись що TrackPoint3D має z як optional:
export interface TrackPoint3D {
    x: number;
    y: number;           // elevation (null для static треків)
    z?: number | null;   // worldZ — null для старих static треків до міграції
}

// TrackLayoutBoundsDto — переконатись що немає minY/maxY:
export interface TrackLayoutBoundsDto {
    minX: number;
    maxX: number;
    minZ: number;
    maxZ: number;
    minElev?: number | null;
    maxElev?: number | null;
}
```

---

### Fix 5 — TrackMap3D: null-safety для z координат (BUG-02 side effect)

**Файл:** `f1-telemetry-web-platform/src/app/components/TrackMap3D.tsx`  
**Складність:** ⭐ (30 хвилин)

```typescript
// TrackMap3D повинна фільтрувати або замінювати z=null:
const validPoints = points
    .filter(p => p.x != null && (p.z != null || p.y != null))
    .map(p => ({
        x: p.x,
        y: p.y ?? 0,      // elevation — 0 якщо null
        z: p.z ?? p.y,    // worldZ — fallback до старого y
    }));
```

---

## 5. Пріоритет виправлень

```
MUST FIX BEFORE MERGE:
  Fix 1 — ViewBox (30 хв)   → трек починає відображатись
  Fix 2 — Migration (15 хв) → всі треки отримують z координати
  Fix 3 — SVG positions (1-2 год) → болідb відображаються
  Fix 4 — TypeScript types (10 хв) → compile safety

SHOULD FIX:
  Fix 5 — 3D null-safety (30 хв) → 3D view для static треків

VERIFY AFTER FIXES:
  □ Трек Sakhir (trackId=3) відображається у 2D
  □ Після запису кола — трек з секторами у кольорах
  □ Болідb видно на карті (хоча б 1 машина)
  □ Export → Import → трек відновився
  □ 3D toggle — трек у 3D без помилок
```

---

## 6. Загальна оцінка

| Категорія | Оцінка |
|-----------|--------|
| Backend архітектура | ⭐⭐⭐⭐ (4/5) — solid, добре структурований |
| Backend completeness | ⭐⭐⭐⭐ (4/5) — всі endpoint є |
| Frontend — логіка | ⭐⭐⭐ (3/5) — є coordinate system mismatch |
| Frontend — UX | ⭐⭐⭐⭐ (4/5) — хороший UI, кнопки на місці |
| Тести | ⭐⭐ (2/5) — не перевірено в PR |
| DB migration | ⭐⭐ (2/5) — неповна (тільки track 8) |
| **Загально** | **⭐⭐⭐ (3/5)** — хороша база, потребує критичних fixes |

**Головна причина проблем:** Зміна `bounds.minY/maxY` → `bounds.minZ/maxZ` в backend DTO не була синхронізована з frontend код, що використовує bounds для SVG viewBox. Це класичний contract mismatch між backend і frontend.

---

*Code review by Claude AI — PR #19 — 2026-03-09*
