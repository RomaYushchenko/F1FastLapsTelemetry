# Implementation Plan: Live Track Map — Telemetry-Based Track Recording (v2)

**Номер плану:** 14  
**Пов'язані плани:** Block F (Steps 21–22), Block H (Steps 27–28)  
**Статус:** Draft  
**Дата:** 2026-03-07  
**Версія:** 6.0 — додано Bulk Export/Import всіх треків одним файлом

---

## 0. Контекст та мотивація

### Проблема

Поточна реалізація Live Track Map використовує `track_layout` таблицю зі **статичними умовними пікселями** (захардкоджений Silverstone). Позиції болідів з гри надходять у **world-координатах гри** (метри, float X/Y/Z), а статичні точки — без зв'язку з ігровим простором. Болід неможливо коректно накласти на карту.

### Рішення: Track Self-Recording + 2D/3D rendering

Записуємо трек у **тій самій системі координат**, що й позиції болідів — X, Y, Z з `PacketMotionData`. Підтримуємо **два режими відображення**:

| Режим | Вісі | Технологія | Коли |
|-------|------|------------|------|
| **2D** (top-down) | X + Z | SVG | за замовчуванням |
| **3D** (perspective) | X + Y + Z | Three.js (`@react-three/fiber`) | за бажанням |

```
┌─────────────────────────────────────────────────────────────────────┐
│  UDP Motion → worldPositionX, worldPositionY, worldPositionZ        │
│  Записуємо всі три осі → зберігаємо в JSONB                         │
│                                                                     │
│  2D: проектуємо XZ (вид зверху)  → SVG path                        │
│  3D: повне XYZ з висотою          → Three.js Line                  │
│                                                                     │
│  Болід: ті самі X,Y,Z → та ж нормалізація → болід ТОЧНО на треку  │
└─────────────────────────────────────────────────────────────────────┘
```

### Система координат гри (F1 25)

```
         Y (вгору, висота / elevation)
         │
         │
         └──────── X (горизонталь, ліво/право)
        /
       /
      Z (горизонталь, вперед/назад)
```

> **Важливо:** `worldPositionY` — це **висота** болідa над поверхнею (elevation).  
> Для 2D top-down view використовуємо **X та Z**.  
> Для 3D view використовуємо **X, Y, Z** де Y = висота.

---

## 1. Вирішені питання (оновлено для v2)

| # | Питання | Рішення |
|---|---------|---------|
| Q1 | Який болід записувати? | Player car — `PacketHeader.m_playerCarIndex` |
| Q2 | Як детектити кінець кола? | `LapDataProcessor` вже детектить — чіпляємось туди |
| Q3 | Частота семплінгу | Кожен 5-й Motion фрейм (~12 Гц) → ~1080 точок/коло |
| Q4 | Де зберігати трек? | `telemetry.track_layout` + нові колонки |
| Q5 | Критерій валідності | ≥ 300 точок + `m_currentLapInvalid = 0` |
| Q6 | Bounds для 2D | `min_x, max_x, min_z, max_z` (горизонтальна площина XZ) |
| Q7 | Bounds для 3D висоти | `min_elev, max_elev` — нові колонки в `track_layout` |
| Q8 | Формат точок в JSONB | `{"x": worldX, "y": worldY, "z": worldZ}` — всі три осі |
| Q9 | Breaking change для Silverstone мок? | Так — міграція 23 оновлює існуючий рядок (додає z=0) |
| Q10 | 3D бібліотека | `@react-three/fiber` + `@react-three/drei` (React wrapper для Three.js) |
| Q11 | Перемикач режимів де? | Toggle button в LiveTrackMap card header |
| Q12 | Болід в 3D | Sphere mesh у тих самих XYZ координатах |
| Q13 | Camera в 3D | OrbitControls — мишею обертати, зумити, панорамувати |
| Q14 | Конфлікт старого формату `{x,y}` → `{x,y,z}` | Міграція 23 перезаписує Silverstone мок; тест на `z` поле |
| Q15 | Джерело меж секторів | `PacketSessionData.sector2LapDistanceStart` / `sector3LapDistanceStart` — вже парсяться в `SessionDataDto`! |
| Q16 | Як прив'язати межу сектору до XYZ позиції? | Зберігати `lapDistance` у кожній точці буфера (`PointXYZD`) → після запису знайти точки з відстанню найближчою до `sector2/3LapDistanceStart` |
| Q17 | Де зберігати межі секторів? | Нова колонка `sector_boundaries JSONB` у `track_layout` |
| Q18 | Формат `sector_boundaries` | `[{sector:1,x,y,z}, {sector:2,x,y,z}, {sector:3,x,y,z}]` — XYZ позиція початку кожного сектору |
| Q19 | Як передати `sector2/3LapDistanceStart` в recording service? | Зберегти в `SessionRuntimeState` при отриманні `SessionDataDto` |
| Q20 | Колір секторів на UI | S1 = зелений `#00FF85`, S2 = жовтий `#FACC15`, S3 = пурпурний `#A855F7` |
| Q21 | Як розфарбувати шлях на SVG? | Знайти індекс точки найближчої до кожної межі → три окремі `<path>` з різними кольорами |

---

## 2. Архітектурна схема (оновлено)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         UDP INGEST SERVICE                               │
│                                                                         │
│  MotionPacketParser → MotionDto                                         │
│    ├─ worldPositionX  ✓ (вже є в MotionDto та парситься)               │
│    ├─ worldPositionY  ✓ (вже є в MotionDto та парситься!)              │
│    └─ worldPositionZ  ✓ (вже є в MotionDto та парситься)               │
│                                                                         │
│  → Kafka: telemetry.motion (без змін в ingest service)                 │
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    TELEMETRY PROCESSING SERVICE                          │
│                                                                         │
│  MotionConsumer                                                         │
│    └─ [EXTEND] передає worldPositionY в TrackLayoutRecordingService     │
│                                                                         │
│  [NEW] TrackLayoutRecordingService                                      │
│    ├─ onSessionStart(sessionUid, trackId)                               │
│    ├─ onMotionFrame(sessionUid, x, y, z, lapDistance) ← y = elevation  │
│    ├─ onLapComplete(sessionUid, lapInvalid)                             │
│    └─ onSessionFinished(sessionUid)                                     │
│                                                                         │
│  [NEW] TrackRecordingState (in SessionRuntimeState)                     │
│    └─ buffer: List<PointXYZ>  ← тепер три координати                   │
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           DATABASE                                       │
│                                                                         │
│  telemetry.track_layout (оновлено)                                     │
│    track_id    SMALLINT PK                                              │
│    points      JSONB  → [{"x":..., "y":..., "z":...}]  ← 3 осі        │
│    version     SMALLINT                                                 │
│    min_x, max_x   DOUBLE PRECISION  (worldPositionX bounds)            │
│    min_y, max_y   DOUBLE PRECISION  (worldPositionZ bounds) *          │
│    min_elev, max_elev  DOUBLE PRECISION  [NEW] (worldPositionY)        │
│    source      VARCHAR(10)  [NEW]  'STATIC' | 'RECORDED'              │
│    recorded_at TIMESTAMPTZ  [NEW]                                       │
│    session_uid BIGINT       [NEW]                                       │
│                                                                         │
│  * Існуючі min_y/max_y зберігають worldPositionZ (горизонталь),       │
│    НЕ висоту. Залишаємо як є — перейменування = breaking change API.  │
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          REST API                                        │
│                                                                         │
│  GET /api/tracks/{trackId}/layout                                       │
│    Response: { trackId, points:[{x,y,z}],                              │
│               bounds:{minX,maxX,minZ,maxZ,minElev,maxElev}, source }   │
│                                                                         │
│  GET /api/tracks/{trackId}/layout/status  [NEW]                        │
│    Response: { trackId, status, pointsCollected, source }              │
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (React)                                 │
│                                                                         │
│  LiveTrackMap                                                           │
│    ├─ viewMode: '2d' | '3d'  (toggle в header)                         │
│    │                                                                    │
│    ├─ [2D mode] TrackMap2D (SVG)                                        │
│    │    ├─ проекція XZ (вид зверху, Y ігнорується)                    │
│    │    ├─ нормалізація XZ → SVG viewport                              │
│    │    └─ болід: circle на нормалізованій XZ позиції                  │
│    │                                                                    │
│    └─ [3D mode] TrackMap3D (@react-three/fiber)                         │
│         ├─ Line (трек як 3D polyline з elevation)                      │
│         ├─ Sphere (болід на XYZ позиції)                               │
│         ├─ OrbitControls (мишею обертати/зумити)                       │
│         └─ нормалізація в [-50, 50] Three.js unit space                │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Алгоритми

### 3.1 State Machine запису (без змін від v1)

```
[IDLE]
   │ onSessionStart AND track NOT in DB
   ▼
[WAITING_FOR_LAP_START]
   │ lapDistance > 0 AND driverStatus = ON_TRACK
   ▼
[RECORDING]  ← addPoint(x, y, z) кожен 5-й фрейм
   ├── lapComplete(invalid=false) AND points ≥ 300 → [SAVING] → [DONE]
   ├── lapComplete(invalid=true) → скинути буфер → [WAITING_FOR_LAP_START]
   └── sessionFinished → [ABORTED]
```

### 3.2 Decimation (без змін)

```
SAMPLE_EVERY = 5  →  ~12 Гц  →  ~1080 точок за 90-секундне коло
```

### 3.3 Обчислення bounds при збереженні

```
// Три незалежних bounds:
minX = min(points[].x)  ;  maxX = max(points[].x)   // worldPositionX
minZ = min(points[].z)  ;  maxZ = max(points[].z)   // worldPositionZ (горизонталь)
minY = min(points[].y)  ;  maxY = max(points[].y)   // worldPositionY (elevation)

// Зберігаємо в таблицю:
track_layout.min_x    = minX  ;  track_layout.max_x    = maxX
track_layout.min_y    = minZ  ;  track_layout.max_y    = maxZ   // існуючі = XZ
track_layout.min_elev = minY  ;  track_layout.max_elev = maxY   // нові = elevation
```

### 3.4 Алгоритм 2D нормалізації (top-down, вид зверху)

```
Проекція: ігноруємо worldPositionY (висоту) → малюємо XZ площину

ВХІД:
  point.x (worldPositionX) — горизонталь ліво/право
  point.z (worldPositionZ) — горизонталь вперед/назад
  bounds: { minX, maxX, minZ (=min_y в DB), maxZ (=max_y в DB) }
  canvas: { width W, height H, padding P }

НОРМАЛІЗАЦІЯ:
  nx = (x - minX) / (maxX - minX) * (W - 2P) + P
  ny = (1 - (z - minZ) / (maxZ - minZ)) * (H - 2P) + P
      ↑ інвертуємо Z бо canvas Y росте вниз

РЕЗУЛЬТАТ: { nx, ny } — пікселі на SVG

Для треку І для болідів — ІДЕНТИЧНІ bounds → болід завжди на треку ✓
```

### 3.5 Алгоритм 3D нормалізації (Three.js scene)

```
Мета: привести всі координати до Three.js unit space [-50, 50]
(щоб камера не літала на мільйони метрів від origin)

ОБЧИСЛЕННЯ ТРАНСФОРМАЦІЇ:
  rangeX = maxX - minX
  rangeZ = maxZ - minZ     (горизонтальна глибина)
  maxRange = max(rangeX, rangeZ)     // один scale для XZ → форма треку збережена
  scale = 100.0 / maxRange

  centerX = (minX + maxX) / 2
  centerY = (minElev + maxElev) / 2  // центр по висоті
  centerZ = (minZ + maxZ) / 2

НОРМАЛІЗАЦІЯ кожної точки:
  threeX =  (point.x - centerX) * scale
  threeY =  (point.y - centerY) * scale   // elevation зберігає пропорцію
  threeZ = -(point.z - centerZ) * scale   // ← ІНВЕРСІЯ Z!
                                           // (Three.js та гра мають протилежні Z)

ПОЧАТКОВА КАМЕРА:
  position: [0, 80, 120]   (над і позаду треку)
  lookAt:   [0, 0, 0]      (центр сцени)
  OrbitControls: обертання + зум + панорамування

ДЛЯ БОЛІДІВ — ті самі centerX/Y/Z та scale → болід на треку ✓
```

### 3.6 Elevation colormap (опційно для 3D)

```
Колір лінії треку залежить від висоти (heatmap):
  нижня точка → синій   (#0066FF)
  середня      → зелений (#00FF85)
  верхня точка → червоний (#E10600)

normalizedElev = (point.y - minElev) / (maxElev - minElev)  // [0..1]
color = interpolateColor(normalizedElev, ['#0066FF', '#00FF85', '#E10600'])

Реалізація: Three.js BufferGeometry з vertexColors
(корисно для трасс з великим перепадом висот: Spa, Suzuka, Austin)
```

---

## 4. База даних — зміни

### 4.1 Міграція `infra/init-db/23-track-layout-recording.sql`

```sql
-- Розширюємо track_layout для auto-recording та 3D elevation

ALTER TABLE telemetry.track_layout
    ADD COLUMN IF NOT EXISTS min_elev    DOUBLE PRECISION NULL,
    ADD COLUMN IF NOT EXISTS max_elev    DOUBLE PRECISION NULL,
    ADD COLUMN IF NOT EXISTS source      VARCHAR(10) NOT NULL DEFAULT 'STATIC',
    ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS session_uid BIGINT NULL;

ALTER TABLE telemetry.track_layout
    ADD CONSTRAINT chk_track_layout_source
    CHECK (source IN ('STATIC', 'RECORDED'));

COMMENT ON COLUMN telemetry.track_layout.min_elev IS
    'Min worldPositionY (elevation) of recorded track in metres';
COMMENT ON COLUMN telemetry.track_layout.max_elev IS
    'Max worldPositionY (elevation) of recorded track in metres';
COMMENT ON COLUMN telemetry.track_layout.source IS
    'STATIC = manually created; RECORDED = auto-recorded from UDP telemetry';

-- Оновлюємо Silverstone мок: конвертуємо {"x","y"} → {"x","y","z"}
-- (старий "y" був worldPositionZ; elevation y = 0 бо flat track)
UPDATE telemetry.track_layout
SET
    points = (
        SELECT jsonb_agg(
            jsonb_build_object(
                'x', (el->>'x')::float,
                'y', 0.0,
                'z', (el->>'y')::float
            )
        )
        FROM jsonb_array_elements(points) AS el
    ),
    min_elev = 0.0,
    max_elev = 0.0
WHERE track_id = 8;
```

### 4.2 Новий формат JSONB точок

```
ДО (v1, плоский 2D):
  [{"x": -234.5, "y": 891.3}, ...]
   x = worldPositionX
   y = worldPositionZ  ← плутанина з назвою!

ПІСЛЯ (v2, повне 3D):
  [{"x": -234.5, "y": 3.2, "z": 891.3}, ...]
   x = worldPositionX   (горизонталь ліво/право)
   y = worldPositionY   (elevation / висота над поверхнею)
   z = worldPositionZ   (горизонталь вперед/назад)
```

---

## 5. Backend — покрокова реалізація

### Step 1: `PointXYZ` record

```java
// .../track/PointXYZ.java
public record PointXYZ(float x, float y, float z) {
    // x = worldPositionX (горизонталь)
    // y = worldPositionY (elevation)
    // z = worldPositionZ (горизонталь глибина)
}
```

### Step 2: `TrackRecordingState` (оновлено)

```java
@Data
public class TrackRecordingState {
    public enum Status {
        IDLE, WAITING_FOR_LAP_START, RECORDING, SAVING, DONE, ABORTED
    }

    private volatile Status status = Status.IDLE;
    private final List<PointXYZ> buffer = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger frameCounter = new AtomicInteger(0);
    private short trackId = -1;

    public boolean shouldSample() {
        return frameCounter.incrementAndGet() % 5 == 0;
    }

    public void addPoint(float x, float y, float z) {  // ← тепер три координати
        buffer.add(new PointXYZ(x, y, z));
    }

    public void reset() {
        buffer.clear();
        frameCounter.set(0);
    }
}
```

### Step 3: `TrackLayoutRecordingService` (оновлено)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackLayoutRecordingService {

    private static final int MIN_POINTS_THRESHOLD = 300;

    private final TrackLayoutRepository trackLayoutRepository;
    private final SessionStateManager sessionStateManager;

    public void onSessionStart(long sessionUid, short trackId) {
        boolean exists = trackLayoutRepository.existsById(trackId);
        TrackRecordingState state = getRecState(sessionUid);
        state.setTrackId(trackId);
        state.setStatus(exists ? Status.IDLE : Status.WAITING_FOR_LAP_START);
        log.info("[TrackRec] trackId={} exists={} → {}", trackId, exists, state.getStatus());
    }

    /**
     * @param worldX  worldPositionX (горизонталь)
     * @param worldY  worldPositionY (elevation) ← NEW
     * @param worldZ  worldPositionZ (горизонталь глибина)
     * @param lapDistance з LapData — для детекту старту кола
     */
    public void onMotionFrame(long sessionUid,
                              float worldX, float worldY, float worldZ,
                              float lapDistance) {
        TrackRecordingState state = getRecState(sessionUid);

        if (state.getStatus() == Status.WAITING_FOR_LAP_START && lapDistance > 0) {
            state.setStatus(Status.RECORDING);
            log.info("[TrackRec] Lap started, trackId={}", state.getTrackId());
        }

        if (state.getStatus() == Status.RECORDING && state.shouldSample()) {
            state.addPoint(worldX, worldY, worldZ);  // ← зберігаємо elevation
        }
    }

    public void onLapComplete(long sessionUid, boolean lapInvalid) {
        TrackRecordingState state = getRecState(sessionUid);
        if (state.getStatus() != Status.RECORDING) return;

        int count = state.getBuffer().size();
        if (lapInvalid || count < MIN_POINTS_THRESHOLD) {
            log.warn("[TrackRec] Lap discarded: invalid={}, points={}", lapInvalid, count);
            state.reset();
            state.setStatus(Status.WAITING_FOR_LAP_START);
            return;
        }

        state.setStatus(Status.SAVING);
        saveTrackLayout(sessionUid, state);
    }

    public void onSessionFinished(long sessionUid) {
        TrackRecordingState state = getRecState(sessionUid);
        if (state.getStatus() == Status.RECORDING
                || state.getStatus() == Status.WAITING_FOR_LAP_START) {
            state.reset();
            state.setStatus(Status.ABORTED);
            log.info("[TrackRec] Session finished — recording aborted");
        }
    }

    private void saveTrackLayout(long sessionUid, TrackRecordingState recState) {
        List<PointXYZ> pts = List.copyOf(recState.getBuffer());
        short trackId = recState.getTrackId();

        // XZ bounds (горизонталь) → в існуючі min_x/min_y/max_x/max_y
        double minX    = pts.stream().mapToDouble(p -> p.x()).min().orElse(0);
        double maxX    = pts.stream().mapToDouble(p -> p.x()).max().orElse(0);
        double minZ    = pts.stream().mapToDouble(p -> p.z()).min().orElse(0);
        double maxZ    = pts.stream().mapToDouble(p -> p.z()).max().orElse(0);

        // Elevation bounds → в нові min_elev/max_elev
        double minElev = pts.stream().mapToDouble(p -> p.y()).min().orElse(0);
        double maxElev = pts.stream().mapToDouble(p -> p.y()).max().orElse(0);

        // JSONB: {"x": worldX, "y": worldY (elevation), "z": worldZ}
        List<Map<String, Double>> jsonPoints = pts.stream()
            .map(p -> Map.of(
                "x", (double) p.x(),
                "y", (double) p.y(),   // elevation!
                "z", (double) p.z()
            ))
            .collect(Collectors.toList());

        TrackLayoutEntity entity = new TrackLayoutEntity();
        entity.setTrackId(trackId);
        entity.setPoints(jsonPoints);
        entity.setVersion((short) 1);
        entity.setMinX(minX);     entity.setMaxX(maxX);
        entity.setMinY(minZ);     entity.setMaxY(maxZ);  // min_y/max_y = worldZ bounds
        entity.setMinElev(minElev); entity.setMaxElev(maxElev);
        entity.setSource("RECORDED");
        entity.setRecordedAt(Instant.now());
        entity.setSessionUid(sessionUid);

        try {
            trackLayoutRepository.save(entity);
            recState.setStatus(Status.DONE);
            log.info("[TrackRec] Saved trackId={}: {} pts, elev=[{:.1f}..{:.1f}]m",
                trackId, pts.size(), minElev, maxElev);
        } catch (Exception e) {
            log.error("[TrackRec] Save failed for trackId={}: {}", trackId, e.getMessage());
            recState.setStatus(Status.ABORTED);
        }
    }

    private TrackRecordingState getRecState(long sessionUid) {
        return sessionStateManager.get(sessionUid).getTrackRecordingState();
    }
}
```

### Step 4: Зміни в `MotionConsumer`

```java
// Додати worldPositionY до виклику recording service:
if (carIndex == playerCarIdx) {
    float lapDistance = state.getLatestLapDistance(carIndex);
    trackLayoutRecordingService.onMotionFrame(
        sessionUid,
        payload.getWorldPositionX(),
        payload.getWorldPositionY(),   // ← NEW: elevation (вже є в MotionDto!)
        payload.getWorldPositionZ(),
        lapDistance
    );
}
```

> `MotionDto.worldPositionY` вже присутній і парситься `MotionPacketParser` —
> **жодних змін в udp-ingest-service не потрібно**.

### Step 5: Оновлення `TrackLayoutEntity`

```java
// Нові поля:
@Column(name = "min_elev")    private Double minElev;
@Column(name = "max_elev")    private Double maxElev;
@Column(name = "source")      private String source;
@Column(name = "recorded_at") private Instant recordedAt;
@Column(name = "session_uid") private Long sessionUid;
```

### Step 6: Оновлення `TrackLayoutResponseDto` та DTO

```java
// TrackPointDto — тепер три поля:
public record TrackPointDto(double x, double y, double z) {
    // x = worldPositionX
    // y = worldPositionY (elevation)
    // z = worldPositionZ
}

// BoundsDto — додати elevation:
public record BoundsDto(
    double minX, double maxX,          // worldPositionX bounds
    double minZ, double maxZ,          // worldPositionZ bounds (горизонталь)
    double minElev, double maxElev     // worldPositionY bounds (висота) ← NEW
) {}

// TrackLayoutResponseDto:
public class TrackLayoutResponseDto {
    private int trackId;
    private List<TrackPointDto> points;  // ← тепер {x, y, z}
    private BoundsDto bounds;
    private String source;               // ← NEW
}
```

> **Breaking change:** `points[].y` тепер означає **elevation** (worldPositionY),  
> а не worldPositionZ як в v1. Frontend оновлюється відповідно.

### Step 7: Status endpoint (без змін від v1)

```java
// TrackLayoutStatusDto:
public record TrackLayoutStatusDto(
    int trackId,
    String status,         // "READY" | "RECORDING" | "NOT_AVAILABLE"
    int pointsCollected,
    String source
) {}

// TrackController:
@GetMapping("/{trackId}/layout/status")
public ResponseEntity<TrackLayoutStatusDto> getLayoutStatus(@PathVariable short trackId) {
    return ResponseEntity.ok(trackLayoutService.getLayoutStatus(trackId));
}
```

---

## 6. Frontend — покрокова реалізація

### Step 8: Оновлення типів

```typescript
// src/api/types.ts

export interface TrackPoint3D {
  x: number;   // worldPositionX (горизонталь)
  y: number;   // worldPositionY (elevation / висота)
  z: number;   // worldPositionZ (горизонталь глибина)
}

export interface TrackBounds {
  minX: number; maxX: number;           // worldPositionX
  minZ: number; maxZ: number;           // worldPositionZ (горизонталь)
  minElev: number; maxElev: number;     // worldPositionY (висота) ← NEW
}

export interface TrackLayoutResponseDto {
  trackId: number;
  points: TrackPoint3D[];               // ← тепер 3D
  bounds?: TrackBounds;
  source?: 'STATIC' | 'RECORDED';
}

export interface TrackLayoutStatusDto {
  trackId: number;
  status: 'READY' | 'RECORDING' | 'NOT_AVAILABLE';
  pointsCollected: number;
  source?: 'STATIC' | 'RECORDED';
}

// CarPositionDto — додати worldPosY для 3D:
export interface CarPositionDto {
  carIndex: number;
  worldPosX: number;
  worldPosY: number;   // ← NEW: elevation
  worldPosZ: number;
  color: string;
  racingNumber?: number;
}
```

### Step 9: `trackNormalization.ts` — 2D і 3D утиліти

```typescript
// src/utils/trackNormalization.ts

import type { TrackPoint3D, TrackBounds } from '@/api/types';

// ─── Shared ────────────────────────────────────────────────────────────────

export function computeBounds(points: TrackPoint3D[]): TrackBounds {
  const xs = points.map(p => p.x);
  const zs = points.map(p => p.z);
  const ys = points.map(p => p.y);  // elevation
  return {
    minX: Math.min(...xs), maxX: Math.max(...xs),
    minZ: Math.min(...zs), maxZ: Math.max(...zs),
    minElev: Math.min(...ys), maxElev: Math.max(...ys),
  };
}

// ─── 2D (top-down, XZ projection) ─────────────────────────────────────────

export interface CanvasConfig {
  width: number;
  height: number;
  padding?: number;
}

/**
 * XZ world-координати → SVG пікселі (вид зверху).
 * worldPositionY (elevation) ігнорується в 2D.
 */
export function normalize2D(
  x: number,
  z: number,
  bounds: TrackBounds,
  canvas: CanvasConfig
): { nx: number; ny: number } {
  const pad = canvas.padding ?? 20;
  const w = canvas.width - 2 * pad;
  const h = canvas.height - 2 * pad;
  const nx = ((x - bounds.minX) / (bounds.maxX - bounds.minX)) * w + pad;
  // Canvas Y росте вниз → інвертуємо Z
  const ny = (1 - (z - bounds.minZ) / (bounds.maxZ - bounds.minZ)) * h + pad;
  return { nx, ny };
}

export function pointsToSvgPath(
  points: TrackPoint3D[],
  bounds: TrackBounds,
  canvas: CanvasConfig
): string {
  if (points.length === 0) return '';
  return (
    points
      .map((p, i) => {
        const { nx, ny } = normalize2D(p.x, p.z, bounds, canvas);
        return `${i === 0 ? 'M' : 'L'}${nx.toFixed(1)},${ny.toFixed(1)}`;
      })
      .join(' ') + ' Z'
  );
}

// ─── 3D (Three.js) ─────────────────────────────────────────────────────────

export interface ThreeTransform {
  centerX: number;
  centerY: number;  // elevation center
  centerZ: number;
  scale: number;    // один масштаб для XZ (зберігає форму треку)
}

/**
 * Обчислює трансформацію для Three.js scene.
 * sceneSize = розмір сцени в Three.js units (default 100).
 * scale береться з max(rangeX, rangeZ) → форма треку не спотворюється.
 * Elevation (Y) масштабується тим самим scale.
 */
export function computeThreeTransform(
  bounds: TrackBounds,
  sceneSize = 100
): ThreeTransform {
  const rangeX = bounds.maxX - bounds.minX;
  const rangeZ = bounds.maxZ - bounds.minZ;
  const maxHRange = Math.max(rangeX, rangeZ, 1);
  return {
    centerX: (bounds.minX + bounds.maxX) / 2,
    centerY: (bounds.minElev + bounds.maxElev) / 2,
    centerZ: (bounds.minZ + bounds.maxZ) / 2,
    scale: sceneSize / maxHRange,
  };
}

/**
 * World XYZ → Three.js [x, y, z].
 * Увага: Three.js Z інвертовано відносно ігрового Z!
 */
export function worldToThree(
  worldX: number,
  worldY: number,
  worldZ: number,
  t: ThreeTransform
): [number, number, number] {
  return [
     (worldX - t.centerX) * t.scale,    // threeX
     (worldY - t.centerY) * t.scale,    // threeY = elevation
    -(worldZ - t.centerZ) * t.scale,    // threeZ = ІНВЕРСІЯ
  ];
}

/**
 * Масив track points → Float32Array для Three.js BufferGeometry.
 * Формат: [x0,y0,z0, x1,y1,z1, ...]
 */
export function pointsToThreeBuffer(
  points: TrackPoint3D[],
  transform: ThreeTransform
): Float32Array {
  const arr = new Float32Array(points.length * 3);
  points.forEach((p, i) => {
    const [tx, ty, tz] = worldToThree(p.x, p.y, p.z, transform);
    arr[i * 3]     = tx;
    arr[i * 3 + 1] = ty;
    arr[i * 3 + 2] = tz;
  });
  return arr;
}

/**
 * Elevation → RGB колір (blue → green → red heatmap).
 */
export function elevationToColor(
  elev: number,
  minElev: number,
  maxElev: number
): string {
  if (maxElev === minElev) return '#6B7280';  // flat track → gray
  const t = (elev - minElev) / (maxElev - minElev);  // [0..1]
  if (t < 0.5) {
    const s = t * 2;
    return `rgb(${Math.round(s * 0)},${Math.round(102 + s * 153)},${Math.round(255 - s * 122)})`;
  } else {
    const s = (t - 0.5) * 2;
    return `rgb(${Math.round(s * 225)},${Math.round(255 - s * 249)},${Math.round(133 - s * 133)})`;
  }
}
```

### Step 10: `TrackMap2D` компонент (SVG)

```tsx
// src/components/live/TrackMap2D.tsx
import type { TrackLayoutResponseDto, CarPositionDto } from '@/api/types';
import { computeBounds, normalize2D, pointsToSvgPath } from '@/utils/trackNormalization';

const SVG_W = 600, SVG_H = 400;
const CANVAS = { width: SVG_W, height: SVG_H, padding: 24 };

export function TrackMap2D({ layout, cars }: {
  layout: TrackLayoutResponseDto;
  cars: CarPositionDto[];
}) {
  const bounds = layout.bounds ?? computeBounds(layout.points);
  const trackPath = pointsToSvgPath(layout.points, bounds, CANVAS);

  return (
    <svg width={SVG_W} height={SVG_H} viewBox={`0 0 ${SVG_W} ${SVG_H}`}
         className="w-full h-auto">
      {/* Підкладка треку (широка) */}
      <path d={trackPath} fill="none" stroke="#374151" strokeWidth={8}
            strokeLinecap="round" strokeLinejoin="round" />
      {/* Лінія треку */}
      <path d={trackPath} fill="none" stroke="#6B7280" strokeWidth={4}
            strokeLinecap="round" strokeLinejoin="round" />

      {/* Болідb */}
      {cars.map(car => {
        const { nx, ny } = normalize2D(car.worldPosX, car.worldPosZ, bounds, CANVAS);
        return (
          <g key={car.carIndex}>
            <circle cx={nx} cy={ny} r={7} fill={car.color} />
            <text x={nx + 9} y={ny + 4} fill={car.color} fontSize={11} fontWeight="bold">
              {car.racingNumber ?? car.carIndex}
            </text>
          </g>
        );
      })}
    </svg>
  );
}
```

### Step 11: `TrackMap3D` компонент (Three.js)

```tsx
// src/components/live/TrackMap3D.tsx
import { Canvas } from '@react-three/fiber';
import { OrbitControls, Line } from '@react-three/drei';
import { useMemo } from 'react';
import type { TrackLayoutResponseDto, CarPositionDto } from '@/api/types';
import {
  computeBounds, computeThreeTransform,
  worldToThree, pointsToThreeBuffer,
} from '@/utils/trackNormalization';

export function TrackMap3D({ layout, cars }: {
  layout: TrackLayoutResponseDto;
  cars: CarPositionDto[];
}) {
  const bounds  = layout.bounds ?? computeBounds(layout.points);
  const transform = useMemo(() => computeThreeTransform(bounds), [bounds]);

  // Масив [x,y,z] для @react-three/drei <Line>
  const trackPoints = useMemo(
    () => layout.points.map(p => worldToThree(p.x, p.y, p.z, transform)),
    [layout.points, transform]
  );

  // Elevation range для title
  const elevRange = bounds.maxElev - bounds.minElev;

  return (
    <div style={{ width: '100%', height: 420 }}>
      <Canvas camera={{ position: [0, 80, 120], fov: 50 }}>
        <ambientLight intensity={0.5} />
        <directionalLight position={[50, 100, 50]} intensity={0.8} />

        {/* Трек як 3D лінія */}
        <Line
          points={trackPoints}
          color="#6B7280"
          lineWidth={3}
        />

        {/* Reference grid на рівні мінімальної висоти */}
        <gridHelper
          args={[120, 30, '#1F2937', '#1F2937']}
          position={[0, (bounds.minElev - bounds.minElev) * transform.scale - 0.5, 0]}
        />

        {/* Болідb */}
        {cars.map(car => {
          const pos = worldToThree(
            car.worldPosX,
            car.worldPosY ?? bounds.minElev,  // fallback якщо Y недоступний
            car.worldPosZ,
            transform
          );
          return (
            <mesh key={car.carIndex} position={pos}>
              <sphereGeometry args={[1.5, 16, 16]} />
              <meshStandardMaterial
                color={car.color}
                emissive={car.color}
                emissiveIntensity={0.4}
              />
            </mesh>
          );
        })}

        <OrbitControls
          enablePan={true}
          enableZoom={true}
          enableRotate={true}
          minDistance={15}
          maxDistance={250}
        />
      </Canvas>

      {/* Elevation info під canvas */}
      {elevRange > 0.5 && (
        <p className="text-xs text-text-secondary mt-1 text-center">
          Elevation change: {elevRange.toFixed(1)} m
        </p>
      )}
    </div>
  );
}
```

### Step 12: `LiveTrackMap` — toggle 2D/3D

```tsx
// src/app/pages/LiveTrackMap.tsx (ключові зміни)

type ViewMode = '2d' | '3d';

export default function LiveTrackMap() {
  const [viewMode, setViewMode] = useState<ViewMode>('2d');
  // ... existing state (layout, layoutStatus, cars, etc.)

  const viewToggle = (
    <div className="flex gap-1 rounded-lg bg-surface-secondary p-1">
      {(['2d', '3d'] as ViewMode[]).map(mode => (
        <button
          key={mode}
          className={`px-3 py-1 rounded text-sm font-medium transition-colors
            ${viewMode === mode
              ? 'bg-accent text-black'
              : 'text-text-secondary hover:text-text-primary'}`}
          onClick={() => setViewMode(mode)}
        >
          {mode.toUpperCase()}
        </button>
      ))}
    </div>
  );

  return (
    <DataCard title={trackName} headerRight={layout ? viewToggle : null}>

      {/* Recording indicator */}
      {layoutStatus?.status === 'RECORDING' && (
        <div className="flex items-center gap-2 text-sm text-yellow-400 mb-3">
          <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
          Recording track layout... {layoutStatus.pointsCollected} points
        </div>
      )}

      {/* Render */}
      {layout && viewMode === '2d' && <TrackMap2D layout={layout} cars={cars} />}
      {layout && viewMode === '3d' && <TrackMap3D layout={layout} cars={cars} />}

      {!layout && layoutStatus?.status === 'NOT_AVAILABLE' && (
        <p className="text-text-secondary text-sm">
          Track layout not yet available. Drive a lap to record it automatically.
        </p>
      )}

    </DataCard>
  );
}
```

---

## 7. Сектори на карті треку

### 7.1 Звідки беруться межі секторів

```
┌────────────────────────────────────────────────────────────────────────┐
│  Два джерела даних:                                                     │
│                                                                        │
│  PacketSessionData (2 Гц)                                              │
│    ├─ sector2LapDistanceStart  → відстань де починається S2 (метри)   │
│    └─ sector3LapDistanceStart  → відстань де починається S3 (метри)   │
│                                                                        │
│  PacketMotionData (60 Гц)                                              │
│    └─ worldPositionX/Y/Z  → XYZ позиція болідa (наш буфер)           │
│                                                                        │
│  Ідея: під час запису зберігаємо lapDistance поряд з кожною XYZ       │
│  точкою. Після кола знаходимо точки найближчі до sector2/3Start.      │
└────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Зміна `PointXYZ` → `PointXYZD`

```java
// БУЛО:
public record PointXYZ(float x, float y, float z) {}

// СТАЄ (додаємо lapDistance):
public record PointXYZD(float x, float y, float z, float lapDistance) {
    // x = worldPositionX
    // y = worldPositionY (elevation)
    // z = worldPositionZ
    // lapDistance = відстань по колу в момент запису точки
}
```

`lapDistance` береться з `SessionRuntimeState.getLatestLapDistance(playerCarIndex)` — той самий getter що вже є в плані.

### 7.3 Алгоритм знаходження меж секторів

```
Виконується ПІСЛЯ завершення запису кола (в saveTrackLayout):

ВХІД:
  buffer: List<PointXYZD>   (1080 точок з lapDistance)
  sector2Start: float        (з SessionRuntimeState, отримано з SessionDataDto)
  sector3Start: float        (теж)

КРОК 1: Знайти точку найближчу до sector2Start
  s2Point = buffer
    .stream()
    .min(comparing(p → |p.lapDistance - sector2Start|))
    .get()

КРОК 2: Знайти точку найближчу до sector3Start
  s3Point = buffer
    .stream()
    .min(comparing(p → |p.lapDistance - sector3Start|))
    .get()

КРОК 3: S1 start = перша точка буфера (start/finish line)
  s1Point = buffer.get(0)

КРОК 4: Побудувати список меж
  boundaries = [
    {sector: 1, x: s1Point.x, y: s1Point.y, z: s1Point.z},
    {sector: 2, x: s2Point.x, y: s2Point.y, z: s2Point.z},
    {sector: 3, x: s3Point.x, y: s3Point.y, z: s3Point.z},
  ]

ЗБЕРЕГТИ в track_layout.sector_boundaries (JSONB)
```

### 7.4 База даних — нова колонка

```sql
-- В міграції 23-track-layout-recording.sql (додати):
ALTER TABLE telemetry.track_layout
    ADD COLUMN IF NOT EXISTS sector_boundaries JSONB NULL;

COMMENT ON COLUMN telemetry.track_layout.sector_boundaries IS
    '[{sector:1|2|3, x, y, z}] — XYZ world position at start of each sector';
```

### 7.5 Зберігання `sector2/3LapDistanceStart` в `SessionRuntimeState`

```java
// SessionRuntimeState — додати поля:
private float sector2LapDistanceStart = -1f;
private float sector3LapDistanceStart = -1f;

// SessionDataConsumer (або SessionConsumer) — при отриманні SessionDataDto:
if (sessionDataDto.getSector2LapDistanceStart() != null) {
    state.setSector2LapDistanceStart(sessionDataDto.getSector2LapDistanceStart());
}
if (sessionDataDto.getSector3LapDistanceStart() != null) {
    state.setSector3LapDistanceStart(sessionDataDto.getSector3LapDistanceStart());
}
```

### 7.6 Оновлення `TrackLayoutRecordingService.saveTrackLayout`

```java
private void saveTrackLayout(long sessionUid, TrackRecordingState recState) {
    List<PointXYZD> pts = List.copyOf(recState.getBuffer());
    SessionRuntimeState sessionState = sessionStateManager.get(sessionUid);

    float s2Dist = sessionState.getSector2LapDistanceStart();
    float s3Dist = sessionState.getSector3LapDistanceStart();

    List<Map<String, Object>> sectorBoundaries = buildSectorBoundaries(pts, s2Dist, s3Dist);

    // ... решта логіки зберігання (без змін) ...
    entity.setSectorBoundaries(sectorBoundaries);  // ← NEW
}

private List<Map<String, Object>> buildSectorBoundaries(
        List<PointXYZD> pts, float s2Dist, float s3Dist) {

    if (pts.isEmpty()) return List.of();

    // S1 start = перша точка = start/finish line
    PointXYZD s1 = pts.get(0);

    // S2 start = точка найближча до sector2LapDistanceStart
    PointXYZD s2 = pts.stream()
        .min(Comparator.comparingDouble(p -> Math.abs(p.lapDistance() - s2Dist)))
        .orElse(pts.get(pts.size() / 2));  // fallback: середина

    // S3 start = точка найближча до sector3LapDistanceStart
    PointXYZD s3 = pts.stream()
        .min(Comparator.comparingDouble(p -> Math.abs(p.lapDistance() - s3Dist)))
        .orElse(pts.get(pts.size() * 2 / 3));  // fallback: 2/3 треку

    return List.of(
        Map.of("sector", 1, "x", s1.x(), "y", s1.y(), "z", s1.z()),
        Map.of("sector", 2, "x", s2.x(), "y", s2.y(), "z", s2.z()),
        Map.of("sector", 3, "x", s3.x(), "y", s3.y(), "z", s3.z())
    );
}
```

### 7.7 API — оновлення `TrackLayoutResponseDto`

```java
// SectorBoundaryDto:
public record SectorBoundaryDto(int sector, double x, double y, double z) {}

// TrackLayoutResponseDto — додати поле:
private List<SectorBoundaryDto> sectorBoundaries;  // ← NEW (null для старих треків)
```

```json
// API response:
{
  "trackId": 7,
  "points": [...],
  "bounds": {...},
  "source": "RECORDED",
  "sectorBoundaries": [
    {"sector": 1, "x": -234.5, "y": 3.2,  "z": 891.3},
    {"sector": 2, "x":  120.1, "y": 5.8,  "z": 1045.2},
    {"sector": 3, "x": -89.4,  "y": 12.1, "z": 760.8}
  ]
}
```

### 7.8 Frontend — кольоровий рендер секторів у 2D

Ідея: знайти індекс точок найближчих до XYZ меж секторів → розбити points на 3 масиви → три `<path>` різних кольорів.

```typescript
// src/utils/trackNormalization.ts — додати функцію:

export const SECTOR_COLORS = {
  1: '#00FF85',  // зелений — S1
  2: '#FACC15',  // жовтий  — S2
  3: '#A855F7',  // пурпурний — S3
} as const;

export interface SectorBoundary {
  sector: 1 | 2 | 3;
  x: number;
  y: number;
  z: number;
}

/**
 * Знаходить індекс точки в масиві найближчої до заданої XZ позиції.
 * Використовується для розбивки треку на сектори.
 */
export function findNearestPointIndex(
  points: TrackPoint3D[],
  targetX: number,
  targetZ: number
): number {
  let bestIdx = 0;
  let bestDist = Infinity;
  points.forEach((p, i) => {
    const d = (p.x - targetX) ** 2 + (p.z - targetZ) ** 2;
    if (d < bestDist) { bestDist = d; bestIdx = i; }
  });
  return bestIdx;
}

/**
 * Розбиває масив точок на 3 сектори.
 * Повертає зрізи [s1Points, s2Points, s3Points].
 */
export function splitIntoSectors(
  points: TrackPoint3D[],
  boundaries: SectorBoundary[]
): [TrackPoint3D[], TrackPoint3D[], TrackPoint3D[]] {
  const s2 = boundaries.find(b => b.sector === 2);
  const s3 = boundaries.find(b => b.sector === 3);

  if (!s2 || !s3 || points.length < 3) {
    return [points, [], []];  // fallback: весь трек як S1
  }

  const s2Idx = findNearestPointIndex(points, s2.x, s2.z);
  const s3Idx = findNearestPointIndex(points, s3.x, s3.z);

  // Впорядковуємо: s2Idx < s3Idx завжди має бути правдою
  const [startS2, startS3] = s2Idx < s3Idx
    ? [s2Idx, s3Idx]
    : [s3Idx, s2Idx];  // fallback swap

  return [
    points.slice(0, startS2 + 1),           // S1: від старту до межі S2
    points.slice(startS2, startS3 + 1),     // S2: від межі S2 до межі S3
    points.slice(startS3),                   // S3: від межі S3 до кінця
  ];
}
```

### 7.9 `TrackMap2D` — рендер кольорових секторів + маркери

```tsx
// src/components/live/TrackMap2D.tsx (оновлено)

export function TrackMap2D({ layout, cars }: Props) {
  const bounds   = layout.bounds ?? computeBounds(layout.points);
  const canvas   = { width: SVG_W, height: SVG_H, padding: 24 };
  const sectors  = layout.sectorBoundaries ?? [];

  // Розбиваємо на три масиви
  const [s1pts, s2pts, s3pts] = splitIntoSectors(layout.points, sectors);

  // Рендер одного сегменту треку
  const renderSegment = (pts: TrackPoint3D[], color: string, closeZ = false) => {
    if (pts.length < 2) return null;
    const d = pts.map((p, i) => {
      const { nx, ny } = normalize2D(p.x, p.z, bounds, canvas);
      return `${i === 0 ? 'M' : 'L'}${nx.toFixed(1)},${ny.toFixed(1)}`;
    }).join(' ') + (closeZ ? ' Z' : '');
    return (
      <>
        {/* Підкладка (широка, темна) */}
        <path d={d} fill="none" stroke="#1F2937" strokeWidth={10}
              strokeLinecap="round" strokeLinejoin="round" />
        {/* Кольорова лінія сектору */}
        <path d={d} fill="none" stroke={color} strokeWidth={4}
              strokeLinecap="round" strokeLinejoin="round" strokeOpacity={0.85} />
      </>
    );
  };

  return (
    <svg width={SVG_W} height={SVG_H} viewBox={`0 0 ${SVG_W} ${SVG_H}`}
         className="w-full h-auto">

      {/* Сектори — три кольорових сегменти */}
      {renderSegment(s1pts, SECTOR_COLORS[1])}
      {renderSegment(s2pts, SECTOR_COLORS[2])}
      {renderSegment(s3pts, SECTOR_COLORS[3], true /* замикає S3 назад до старту */)}

      {/* Маркери меж секторів */}
      {sectors.map(b => {
        if (b.sector === 1) return null;  // S1 старт = старт/фініш = окремий маркер
        const { nx, ny } = normalize2D(b.x, b.z, bounds, canvas);
        const color = SECTOR_COLORS[b.sector as 2 | 3];
        return (
          <g key={`sector-${b.sector}`}>
            {/* Ромб-маркер */}
            <polygon
              points={`${nx},${ny - 10} ${nx + 7},${ny} ${nx},${ny + 10} ${nx - 7},${ny}`}
              fill={color}
              stroke="#111827"
              strokeWidth={1.5}
            />
            {/* Підпис "S2" / "S3" */}
            <text x={nx + 11} y={ny + 4} fill={color}
                  fontSize={11} fontWeight="bold">
              S{b.sector}
            </text>
          </g>
        );
      })}

      {/* Старт/фініш маркер */}
      {sectors.find(b => b.sector === 1) && (() => {
        const s1 = sectors.find(b => b.sector === 1)!;
        const { nx, ny } = normalize2D(s1.x, s1.z, bounds, canvas);
        return (
          <g>
            <rect x={nx - 4} y={ny - 10} width={8} height={20}
                  fill="#FFFFFF" stroke="#111827" strokeWidth={1.5} rx={1} />
            <text x={nx + 8} y={ny + 4} fill="#FFFFFF"
                  fontSize={10} fontWeight="bold">
              S/F
            </text>
          </g>
        );
      })()}

      {/* Легенда секторів */}
      <g transform="translate(12, 12)">
        {([1, 2, 3] as const).map((s, i) => (
          <g key={s} transform={`translate(0, ${i * 18})`}>
            <rect width={14} height={6} y={1} rx={3}
                  fill={SECTOR_COLORS[s]} />
            <text x={18} y={9} fill="#9CA3AF" fontSize={10}>
              Sector {s}
            </text>
          </g>
        ))}
      </g>

      {/* Болідb */}
      {cars.map(car => {
        const { nx, ny } = normalize2D(car.worldPosX, car.worldPosZ, bounds, canvas);
        return (
          <g key={car.carIndex}>
            <circle cx={nx} cy={ny} r={7} fill={car.color} />
            <text x={nx + 9} y={ny + 4} fill={car.color}
                  fontSize={11} fontWeight="bold">
              {car.racingNumber ?? car.carIndex}
            </text>
          </g>
        );
      })}
    </svg>
  );
}
```

### 7.10 `TrackMap3D` — кольорові сегменти в Three.js

```tsx
// src/components/live/TrackMap3D.tsx (оновлено — sector lines)

export function TrackMap3D({ layout, cars }: Props) {
  const bounds    = layout.bounds ?? computeBounds(layout.points);
  const transform = useMemo(() => computeThreeTransform(bounds), [bounds]);
  const sectors   = layout.sectorBoundaries ?? [];

  // Розбиваємо і конвертуємо кожен сектор у Three.js точки
  const [s1pts, s2pts, s3pts] = splitIntoSectors(layout.points, sectors);

  const toThreePoints = (pts: TrackPoint3D[]) =>
    pts.map(p => worldToThree(p.x, p.y, p.z, transform));

  return (
    <div style={{ width: '100%', height: 420 }}>
      <Canvas camera={{ position: [0, 80, 120], fov: 50 }}>
        <ambientLight intensity={0.5} />
        <directionalLight position={[50, 100, 50]} intensity={0.8} />

        {/* Три кольорових відрізки треку */}
        {s1pts.length > 1 && (
          <Line points={toThreePoints(s1pts)} color={SECTOR_COLORS[1]} lineWidth={4} />
        )}
        {s2pts.length > 1 && (
          <Line points={toThreePoints(s2pts)} color={SECTOR_COLORS[2]} lineWidth={4} />
        )}
        {s3pts.length > 1 && (
          <Line points={toThreePoints(s3pts)} color={SECTOR_COLORS[3]} lineWidth={4} />
        )}

        {/* Маркери меж секторів — сфери + текст */}
        {sectors.filter(b => b.sector !== 1).map(b => {
          const pos = worldToThree(b.x, b.y, b.z, transform);
          const color = SECTOR_COLORS[b.sector as 2 | 3];
          return (
            <group key={`s${b.sector}`} position={pos}>
              <mesh>
                <sphereGeometry args={[2, 12, 12]} />
                <meshStandardMaterial color={color} emissive={color} emissiveIntensity={0.6} />
              </mesh>
              {/* Текст "S2" / "S3" над маркером */}
              <Html position={[0, 4, 0]} center>
                <span style={{
                  color, fontSize: 12, fontWeight: 'bold',
                  background: 'rgba(0,0,0,0.6)', padding: '1px 4px', borderRadius: 3
                }}>
                  S{b.sector}
                </span>
              </Html>
            </group>
          );
        })}

        {/* Start/Finish маркер */}
        {sectors.find(b => b.sector === 1) && (() => {
          const s1 = sectors.find(b => b.sector === 1)!;
          const pos = worldToThree(s1.x, s1.y, s1.z, transform);
          return (
            <group position={pos}>
              <mesh>
                <boxGeometry args={[1, 4, 0.5]} />
                <meshStandardMaterial color="#FFFFFF" />
              </mesh>
              <Html position={[0, 6, 0]} center>
                <span style={{
                  color: '#FFFFFF', fontSize: 11, fontWeight: 'bold',
                  background: 'rgba(0,0,0,0.6)', padding: '1px 4px', borderRadius: 3
                }}>S/F</span>
              </Html>
            </group>
          );
        })()}

        {/* Болідb */}
        {cars.map(car => {
          const pos = worldToThree(car.worldPosX, car.worldPosY ?? bounds.minElev, car.worldPosZ, transform);
          return (
            <mesh key={car.carIndex} position={pos}>
              <sphereGeometry args={[1.5, 16, 16]} />
              <meshStandardMaterial color={car.color} emissive={car.color} emissiveIntensity={0.4} />
            </mesh>
          );
        })}

        <gridHelper args={[120, 30, '#1F2937', '#1F2937']} />
        <OrbitControls enablePan enableZoom enableRotate minDistance={15} maxDistance={250} />
      </Canvas>
    </div>
  );
}
```

---

## 8. Експорт та імпорт треку (Dev-утиліта)

### 8.1 Мотивація та workflow

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Dev workflow (часті перезапуски з чистою БД):                          │
│                                                                         │
│  1. Записати трек (перше коло) → збережено в track_layout в БД         │
│  2. [UI] Натиснути "Export" → завантажується track-7-spa.json          │
│  3. Зупинити весь стек (Kafka + PostgreSQL + Spring)                   │
│  4. Запустити знову → БД чиста                                          │
│  5. [UI] Натиснути "Import" → обрати файл → трек відновлено в БД       │
│  6. Наступна гра — трек вже є, recording mode не активується           │
└─────────────────────────────────────────────────────────────────────────┘
```

### 8.2 Формат JSON файлу

```json
{
  "exportVersion": 1,
  "exportedAt": "2026-03-09T14:23:11Z",
  "trackId": 7,
  "trackName": "Spa-Francorchamps",
  "version": 1,
  "source": "RECORDED",
  "points": [
    {"x": -234.5, "y": 3.2, "z": 891.3},
    {"x": -231.2, "y": 3.4, "z": 895.1}
  ],
  "bounds": {
    "minX": -450.0, "maxX": 350.0,
    "minZ": 600.0,  "maxZ": 1200.0,
    "minElev": 0.0, "maxElev": 28.5
  },
  "sectorBoundaries": [
    {"sector": 1, "x": -234.5, "y": 3.2, "z": 891.3},
    {"sector": 2, "x":  120.1, "y": 5.8, "z": 1045.2},
    {"sector": 3, "x":  -89.4, "y": 12.1, "z": 760.8}
  ]
}
```

> Поле `trackName` — для зручності читання файлу. При імпорті ігнорується  
> (авторитетним є `trackId`). `exportVersion` дозволяє в майбутньому  
> мігрувати формат без breaking changes.

### 8.3 Backend — Export endpoint

```
GET /api/tracks/{trackId}/layout/export
Response 200: Content-Type: application/json
              Content-Disposition: attachment; filename="track-{trackId}-layout.json"
              Body: TrackLayoutExportDto (JSON)
Response 404: layout не знайдено
```

```java
// TrackController:
@GetMapping("/{trackId}/layout/export")
public ResponseEntity<TrackLayoutExportDto> exportLayout(
        @PathVariable Short trackId) {
    log.debug("exportLayout: trackId={}", trackId);
    return trackLayoutService.exportLayout(trackId)
        .map(dto -> ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"track-" + trackId + "-layout.json\"")
            .body(dto))
        .orElseGet(() -> ResponseEntity.notFound().build());
}

// TrackLayoutService:
public Optional<TrackLayoutExportDto> exportLayout(Short trackId) {
    if (trackId == null) return Optional.empty();
    return trackLayoutRepository.findById(trackId)
        .map(entity -> TrackLayoutExportDto.builder()
            .exportVersion(1)
            .exportedAt(Instant.now().toString())
            .trackId(entity.getTrackId().intValue())
            .trackName(getTrackName(trackId))  // з constants/tracks
            .version(entity.getVersion() != null ? entity.getVersion().intValue() : 1)
            .source(entity.getSource())
            .points(parsePoints(entity.getPointsJson()))
            .bounds(buildBoundsDto(entity))
            .sectorBoundaries(parseSectorBoundaries(entity.getSectorBoundariesJson()))
            .build());
}
```

### 8.4 Backend — Import endpoint

```
POST /api/tracks/layout/import
Content-Type: application/json
Body: TrackLayoutExportDto (той самий формат що й export)

Response 200: TrackLayoutResponseDto (збережений стан)
Response 400: невалідний JSON або відсутній trackId
```

```java
// TrackController:
@PostMapping("/layout/import")
public ResponseEntity<TrackLayoutResponseDto> importLayout(
        @RequestBody @Valid TrackLayoutImportDto dto) {
    log.info("importLayout: trackId={}, source={}, points={}",
        dto.getTrackId(), dto.getSource(), dto.getPoints().size());
    TrackLayoutResponseDto saved = trackLayoutService.importLayout(dto);
    return ResponseEntity.ok(saved);
}

// TrackLayoutService:
@Transactional
public TrackLayoutResponseDto importLayout(TrackLayoutImportDto dto) {
    // Upsert: JPA save() з існуючим PK = UPDATE, новий = INSERT
    TrackLayout entity = TrackLayout.builder()
        .trackId(dto.getTrackId().shortValue())
        .pointsJson(serializePoints(dto.getPoints()))
        .version(dto.getVersion() != null ? dto.getVersion().shortValue() : 1)
        .minX(dto.getBounds() != null ? dto.getBounds().getMinX() : null)
        .minY(dto.getBounds() != null ? dto.getBounds().getMinZ() : null)  // minZ → minY
        .maxX(dto.getBounds() != null ? dto.getBounds().getMaxX() : null)
        .maxY(dto.getBounds() != null ? dto.getBounds().getMaxZ() : null)  // maxZ → maxY
        .minElev(dto.getBounds() != null ? dto.getBounds().getMinElev() : null)
        .maxElev(dto.getBounds() != null ? dto.getBounds().getMaxElev() : null)
        .sectorBoundariesJson(serializeSectorBoundaries(dto.getSectorBoundaries()))
        .source(dto.getSource() != null ? dto.getSource() : "IMPORTED")
        .build();

    TrackLayout saved = trackLayoutRepository.save(entity);  // upsert via PK
    log.info("importLayout: saved trackId={} ({} points)", saved.getTrackId(),
        dto.getPoints().size());
    return toDto(saved);
}
```

> **Upsert через JPA:** `TrackLayoutRepository.save(entity)` з існуючим `track_id` (PK)  
> автоматично виконає `UPDATE`. Новий `track_id` → `INSERT`. Жодного додаткового SQL.

### 8.5 DTOs

```java
// TrackLayoutExportDto (використовується і для export response і як import body):
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrackLayoutExportDto {
    private int exportVersion;         // завжди 1 поки що
    private String exportedAt;         // ISO-8601, для info
    private int trackId;
    private String trackName;          // для читабельності файлу; ігнорується при import
    private int version;
    private String source;             // "RECORDED" | "STATIC" | "IMPORTED"
    private List<TrackPointDto> points;
    private TrackLayoutBoundsExportDto bounds;
    private List<SectorBoundaryDto> sectorBoundaries;
}

// TrackLayoutImportDto — alias або той самий клас (без exportedAt валідації):
// Для простоти: той самий TrackLayoutExportDto приймається як @RequestBody
// @Valid перевіряє: trackId != null, points != null && !empty

// TrackLayoutBoundsExportDto — оновлений bounds з elevation:
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrackLayoutBoundsExportDto {
    private double minX; private double maxX;
    private double minZ; private double maxZ;     // worldPositionZ bounds
    private double minElev; private double maxElev; // elevation bounds
}
```

### 8.6 Frontend — Export кнопка

```tsx
// Показується в header LiveTrackMap ТІЛЬКИ якщо layout завантажено
// і source !== 'STATIC' (тобто є що експортувати)

const handleExport = () => {
  // Простий підхід: відкрити URL → браузер завантажить файл
  window.open(
    `/api/tracks/${trackId}/layout/export`,
    '_blank'
  )
}

// В header LiveTrackMap:
const headerActions = layout ? (
  <div className="flex items-center gap-2">
    {/* Export кнопка */}
    <button
      onClick={handleExport}
      className="flex items-center gap-1.5 px-2 py-1 rounded text-xs
                 text-text-secondary hover:text-text-primary
                 hover:bg-surface-secondary transition-colors"
      title="Export track layout to JSON"
    >
      <Download size={14} />
      Export
    </button>

    {/* 2D / 3D toggle */}
    <div className="flex gap-1 rounded-lg bg-surface-secondary p-1">
      {(['2d', '3d'] as ViewMode[]).map(mode => (
        <button key={mode} onClick={() => setViewMode(mode)}
          className={`px-3 py-1 rounded text-sm font-medium transition-colors
            ${viewMode === mode ? 'bg-accent text-black' : 'text-text-secondary'}`}>
          {mode.toUpperCase()}
        </button>
      ))}
    </div>
  </div>
) : null
```

### 8.7 Frontend — Import (коли layout відсутній)

```tsx
// Показується замість/поряд з "NOT_AVAILABLE" та "RECORDING" станами

const handleImport = async (file: File) => {
  if (!file.name.endsWith('.json')) {
    toast.error('Please select a JSON file')
    return
  }

  try {
    const text = await file.text()
    const data = JSON.parse(text)

    const response = await fetch('/api/tracks/layout/import', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    })

    if (!response.ok) {
      throw new Error(`Import failed: ${response.status}`)
    }

    toast.success(`Track layout imported for track ${data.trackId}`)
    // Перезавантажити layout:
    const layout = await getTrackLayout(trackId!)
    if (layout) setLayout(layout)

  } catch (err) {
    toast.error(err instanceof Error ? err.message : 'Import failed')
  }
}

// UI — компонент для стану "немає layout" (NOT_AVAILABLE або початковий):
{!layout && layoutStatus?.status !== 'RECORDING' && (
  <div className="flex flex-col items-center gap-4 py-8">

    {/* Основне повідомлення */}
    <p className="text-text-secondary text-sm text-center">
      Track layout not yet available.
      {layoutStatus?.status === 'NOT_AVAILABLE'
        ? ' Drive a lap to record it automatically.'
        : ''}
    </p>

    {/* Import секція */}
    <div className="flex flex-col items-center gap-2">
      <p className="text-text-secondary text-xs">
        Or import a previously exported layout:
      </p>
      <label className="cursor-pointer">
        <input
          type="file"
          accept=".json"
          className="hidden"
          onChange={e => {
            const file = e.target.files?.[0]
            if (file) handleImport(file)
            e.target.value = ''  // reset щоб можна було re-import той самий файл
          }}
        />
        <span className="flex items-center gap-1.5 px-3 py-1.5 rounded
                         border border-surface-border text-xs text-text-secondary
                         hover:text-text-primary hover:border-accent transition-colors">
          <Upload size={13} />
          Import track JSON
        </span>
      </label>
    </div>

  </div>
)}
```

### 8.8 Іконки та imports

```typescript
// Потрібні іконки з lucide-react (вже є в проекті):
import { Download, Upload } from 'lucide-react'
```

### 8.9 Ім'я файлу при імпорті — валідація trackId

```
При імпорті перевіряємо:
  1. Файл є валідним JSON
  2. trackId є числом і > 0
  3. points є масивом з хоча б однією точкою
  4. Якщо trackId у файлі != trackId поточної сесії → попередити але дозволити
     (може бути корисно: перенести трек на інший trackId)

Якщо trackId != trackId поточної сесії:
  → Toast: "Warning: importing track {fileTrackId} but current session is on track {sessionTrackId}"
  → Продовжити без блокування
```

### 8.10 Bulk Export — всі треки одним файлом

**Проблема:** при частих перезапусках з чистою БД зручніше зберігати **один файл** зі всіма записаними треками, а не окремий файл per track.

```
GET /api/tracks/layout/export-all
Response 200: Content-Type: application/json
              Content-Disposition: attachment; filename="all-tracks-layout.json"
              Body: TrackLayoutBulkExportDto

Response 200 (порожній список): { exportVersion: 1, exportedAt: "...", tracks: [] }
Завжди 200 — навіть якщо в БД нема жодного треку.
```

```java
// TrackController:
@GetMapping("/layout/export-all")
public ResponseEntity<TrackLayoutBulkExportDto> exportAllLayouts() {
    log.debug("exportAllLayouts");
    TrackLayoutBulkExportDto bulk = trackLayoutService.exportAllLayouts();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"all-tracks-layout.json\"")
        .body(bulk);
}

// TrackLayoutService:
public TrackLayoutBulkExportDto exportAllLayouts() {
    // trackLayoutRepository.findAll() — безкоштовно, JPA вже є
    List<TrackLayoutExportDto> tracks = trackLayoutRepository.findAll()
        .stream()
        .map(entity -> toExportDto(entity))  // той самий метод що й для single export
        .sorted(Comparator.comparingInt(TrackLayoutExportDto::getTrackId))
        .collect(Collectors.toList());

    log.info("exportAllLayouts: exporting {} tracks", tracks.size());
    return TrackLayoutBulkExportDto.builder()
        .exportVersion(1)
        .exportedAt(Instant.now().toString())
        .count(tracks.size())
        .tracks(tracks)
        .build();
}
```

### 8.11 Bulk Import — завантаження всіх треків з файлу

```
POST /api/tracks/layout/import-all
Content-Type: application/json
Body: TrackLayoutBulkExportDto

Response 200: { imported: 3, skipped: 0, errors: [] }
Response 400: невалідний JSON або відсутнє поле tracks
```

```java
// TrackController:
@PostMapping("/layout/import-all")
public ResponseEntity<BulkImportResultDto> importAllLayouts(
        @RequestBody TrackLayoutBulkExportDto dto) {
    log.info("importAllLayouts: {} tracks", dto.getTracks() != null ? dto.getTracks().size() : 0);
    BulkImportResultDto result = trackLayoutService.importAllLayouts(dto);
    return ResponseEntity.ok(result);
}

// TrackLayoutService:
@Transactional
public BulkImportResultDto importAllLayouts(TrackLayoutBulkExportDto dto) {
    if (dto.getTracks() == null || dto.getTracks().isEmpty()) {
        return BulkImportResultDto.builder().imported(0).skipped(0).errors(List.of()).build();
    }

    int imported = 0;
    List<String> errors = new ArrayList<>();

    for (TrackLayoutExportDto track : dto.getTracks()) {
        try {
            if (track.getTrackId() <= 0 || track.getPoints() == null || track.getPoints().isEmpty()) {
                errors.add("Track " + track.getTrackId() + ": invalid data (no points)");
                continue;
            }
            importLayout(track);  // той самий upsert що й для single import
            imported++;
        } catch (Exception e) {
            log.warn("importAllLayouts: failed for trackId={}: {}", track.getTrackId(), e.getMessage());
            errors.add("Track " + track.getTrackId() + ": " + e.getMessage());
        }
    }

    log.info("importAllLayouts: imported={}, errors={}", imported, errors.size());
    return BulkImportResultDto.builder()
        .imported(imported)
        .skipped(errors.size())
        .errors(errors)
        .build();
}
```

### 8.12 DTO для bulk операцій

```java
// TrackLayoutBulkExportDto:
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrackLayoutBulkExportDto {
    private int exportVersion;       // 1
    private String exportedAt;       // ISO-8601
    private int count;               // кількість треків для зручності
    private List<TrackLayoutExportDto> tracks;  // ті самі TrackLayoutExportDto
}

// BulkImportResultDto:
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BulkImportResultDto {
    private int imported;
    private int skipped;
    private List<String> errors;
}
```

### 8.13 Frontend — Export All і Import All

```tsx
// Окрема "Dev Tools" секція або кнопка в settings / header
// Логіка — мінімальна: ті самі підходи що і для single

// Export All:
const handleExportAll = () => {
  window.open('/api/tracks/layout/export-all', '_blank')
}

// Import All:
const handleImportAll = async (file: File) => {
  try {
    const text = await file.text()
    const data = JSON.parse(text)

    // Валідація: перевіряємо що це bulk файл
    if (!Array.isArray(data.tracks)) {
      // Можливо це single-track файл — fallback до single import
      await handleImport(file)
      return
    }

    const res = await fetch('/api/tracks/layout/import-all', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    })
    const result: BulkImportResult = await res.json()

    if (result.errors.length > 0) {
      toast.warning(
        `Imported ${result.imported} tracks. ${result.skipped} failed: ${result.errors[0]}`
      )
    } else {
      toast.success(`Imported ${result.imported} track layouts`)
    }

    // Якщо trackId поточної сесії є у файлі — перезавантажити layout
    const hasCurrentTrack = data.tracks.some(
      (t: { trackId: number }) => t.trackId === trackId
    )
    if (hasCurrentTrack && trackId) {
      const layout = await getTrackLayout(trackId)
      if (layout) setLayout(layout)
    }

  } catch (err) {
    toast.error(err instanceof Error ? err.message : 'Import failed')
  }
}
```

### 8.14 Де розмістити кнопки в UI

```
Рекомендований підхід: об'єднати single і bulk в одному місці.

┌─────────────────────────────────────────────────────┐
│  LiveTrackMap header (коли layout є):               │
│                                                     │
│  [Export]  [Export All]  [2D] [3D]                 │
│                                                     │
│  LiveTrackMap header (коли layout відсутній):       │
│                                                     │
│  "Drive a lap to record..."                         │
│  [Import track JSON]  [Import All Tracks]           │
└─────────────────────────────────────────────────────┘

Примітки:
- "Export" → single track (поточний trackId)
- "Export All" → bulk (всі треки в БД)
- "Import track JSON" → single (один файл per track)
- "Import All Tracks" → bulk (файл export-all з усіма треками)
- Import доступний ЗАВЖДИ (і коли layout є, і коли нема)
  → hover menu або окрема іконка щоб не переповнювати header
```

---

## 9. Recording UI Indicator — детальний алгоритм

### 9.1 Всі можливі стани сторінки LiveTrackMap

```
┌────────────────────────────────────────────────────────────────────────────┐
│                     СТАНИ LiveTrackMap                                     │
│                                                                            │
│  A. Немає активної сесії                                                   │
│     session = null (з useLiveTelemetry)                                    │
│     → "No active session. Start F1 25 to continue."                       │
│                                                                            │
│  B. Сесія є, layout вже є в БД                                             │
│     status.status = "READY"                                                │
│     → Одразу малюємо трек. Немає recording indicator.                     │
│                                                                            │
│  C. Сесія є, запис іде (RECORDING)                                        │
│     status.status = "RECORDING"                                            │
│     → Recording indicator (пульсуюча точка + лічильник)                   │
│     → Polling кожні 2 сек → коли status стає READY → fetch layout →       │
│       трек з'являється автоматично (без перезавантаження!)                │
│                                                                            │
│  D. Сесія є, трек ще не записаний, запис не почався                       │
│     status.status = "NOT_AVAILABLE"                                        │
│     → "Track layout not yet available. Drive a lap to record it."         │
│     → Polling кожні 4 сек → коли з'явиться RECORDING → перехід в С       │
│                                                                            │
│  E. Layout завантажується (loading)                                        │
│     isLoadingLayout = true                                                 │
│     → Skeleton / spinner                                                   │
└────────────────────────────────────────────────────────────────────────────┘
```

### 9.2 Recording indicator — UI

```tsx
// Показується коли layoutStatus.status === 'RECORDING'

<div className="flex flex-col items-center justify-center gap-4 py-8">

  {/* Пульсуюча анімація запису */}
  <div className="relative flex items-center justify-center">
    <div className="w-16 h-16 rounded-full bg-red-500/10 animate-ping absolute" />
    <div className="w-8 h-8 rounded-full bg-red-500/30 animate-ping absolute" />
    <div className="w-4 h-4 rounded-full bg-red-500" />
  </div>

  <div className="text-center">
    <p className="text-text-primary font-medium">Recording track layout...</p>
    <p className="text-text-secondary text-sm mt-1">
      Drive one full lap to generate the track map
    </p>
  </div>

  {/* Progress bar — прогрес до мінімальних 300 точок */}
  <div className="w-64">
    <div className="flex justify-between text-xs text-text-secondary mb-1">
      <span>Progress</span>
      <span>{layoutStatus.pointsCollected} / 300+ points</span>
    </div>
    <div className="h-2 bg-surface-secondary rounded-full overflow-hidden">
      <div
        className="h-full bg-yellow-400 rounded-full transition-all duration-500"
        style={{ width: `${Math.min(100, (layoutStatus.pointsCollected / 300) * 100)}%` }}
      />
    </div>
  </div>

  <p className="text-xs text-text-secondary">
    Track map will appear automatically when the lap is complete
  </p>
</div>
```

### 9.3 Автоматичний перехід з RECORDING → READY (без перезавантаження)

Алгоритм:
1. Polling `/layout/status` кожні 2 сек поки `status !== 'READY'`
2. Коли `status` стає `READY` → негайно викликаємо `getTrackLayout(trackId)` → записуємо в `layout` state
3. React перерендерить компонент → indicator зникає → трек з'являється

```typescript
// Детальна реалізація в LiveTrackMap:

const STATUS_POLL_INTERVAL_RECORDING = 2000   // швидше під час запису
const STATUS_POLL_INTERVAL_NOT_AVAILABLE = 4000 // повільніше якщо ще не стартував

useEffect(() => {
  if (!trackId || layout) return  // трек вже є — не поллимо

  let cancelled = false

  const poll = async () => {
    if (cancelled) return

    const status = await getTrackLayoutStatus(trackId)
    if (cancelled) return

    setLayoutStatus(status)

    if (status?.status === 'READY') {
      // Трек з'явився — завантажуємо одразу
      const data = await getTrackLayout(trackId)
      if (!cancelled && data) {
        setLayout(data)  // ← React перерендерить → трек на екрані
      }
      return  // Зупиняємо polling
    }

    // Підбираємо інтервал залежно від стану
    const interval = status?.status === 'RECORDING'
      ? STATUS_POLL_INTERVAL_RECORDING
      : STATUS_POLL_INTERVAL_NOT_AVAILABLE

    pollRef.current = setTimeout(poll, interval)
  }

  poll()
  return () => {
    cancelled = true
    clearTimeout(pollRef.current)
  }
}, [trackId, layout])
```

---

## 10. Авто-реактивність сторінки — детальний аналіз

### 10.1 Існуюча інфраструктура

`LiveTelemetryProvider` вже реалізує весь необхідний lifecycle **автоматично**:

```
LiveTelemetryProvider (mount, single instance для всього додатку)
   │
   ├─ getActiveSession() → null
   │     ↓ 4 сек
   ├─ getActiveSession() → null
   │     ↓ 4 сек
   ├─ getActiveSession() → { id, trackId, ... }  ← сесія з'явилась!
   │     ↓
   ├─ connectWithSession(activeSession) → STOMP WS
   │     ↓
   ├─ status: 'live', session: { id, trackId }
   │
   └─ Коли SESSION_ENDED →
         status: 'disconnected'
         ↓ 4 сек
         getActiveSession() → polling знову...
```

`useLiveTelemetry()` в `LiveTrackMap` отримує ці зміни через React context → **компонент автоматично перерендериться**.

### 10.2 Реактивні ефекти в LiveTrackMap

```
Тригер                           Ефект
──────────────────────────────   ────────────────────────────────────────────
session змінився (null → об'єкт) fetchLayout(session.trackId) або startPollingStatus()
trackId змінився                 скинути layout, скинути status, почати все знову
layout змінився (null → об'єкт)  зупинити polling, відрендерити трек
status === 'POSITIONS'           оновити positions болідів (з WS)
session став null                показати "No active session"
```

### 10.3 Повна реактивна логіка `LiveTrackMap`

```typescript
export default function LiveTrackMap() {
  const { session, status: wsStatus } = useLiveTelemetry()

  // ─── Derived state ─────────────────────────────────────────────────────
  const trackId = session?.trackId ?? null

  // ─── Local state ───────────────────────────────────────────────────────
  const [layout, setLayout]               = useState<TrackLayoutResponseDto | null>(null)
  const [layoutStatus, setLayoutStatus]   = useState<TrackLayoutStatusDto | null>(null)
  const [isLoadingLayout, setIsLoadingLayout] = useState(false)
  const [cars, setCars]                   = useState<CarPositionDto[]>([])
  const [viewMode, setViewMode]           = useState<'2d' | '3d'>('2d')
  const pollRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // ─── Ефект 1: trackId змінився ─────────────────────────────────────────
  // Скидаємо всі дані і починаємо все з нуля.
  // Це відбувається коли:
  //   - Сесія з'явилась (null → об'єкт з trackId)
  //   - Гравець перейшов на інший трек між сесіями
  //   - Сесія завершилась (trackId стає null) → показуємо "No active session"
  useEffect(() => {
    // Скидаємо стару карту і статус
    setLayout(null)
    setLayoutStatus(null)
    setIsLoadingLayout(false)
    clearTimeout(pollRef.current ?? undefined)

    if (!trackId) return  // Сесія зникла → нічого не робимо

    // Спробувати одразу завантажити layout
    const tryFetchLayout = async () => {
      setIsLoadingLayout(true)
      try {
        const data = await getTrackLayout(trackId)
        if (data) {
          setLayout(data)        // Трек є в БД → одразу відображаємо
          setIsLoadingLayout(false)
          return
        }
      } catch {
        // Layout відсутній (404) — йдемо до polling status
      }
      setIsLoadingLayout(false)
      // Layout ще немає — запускаємо polling status
    }

    tryFetchLayout()
  }, [trackId])

  // ─── Ефект 2: Polling status поки layout не з'явився ──────────────────
  useEffect(() => {
    if (!trackId || layout) return  // Якщо трек вже є — не поллимо

    let cancelled = false
    const poll = async () => {
      if (cancelled) return
      const st = await getTrackLayoutStatus(trackId)
      if (cancelled) return

      setLayoutStatus(st)

      if (st?.status === 'READY') {
        const data = await getTrackLayout(trackId)
        if (!cancelled && data) setLayout(data)
        return  // Зупиняємо polling
      }

      const interval = st?.status === 'RECORDING' ? 2000 : 4000
      pollRef.current = setTimeout(poll, interval)
    }

    poll()
    return () => {
      cancelled = true
      clearTimeout(pollRef.current ?? undefined)
    }
  }, [trackId, layout])

  // ─── Ефект 3: Позиції болідів з WebSocket ──────────────────────────────
  // LiveTelemetryProvider вже отримує POSITIONS повідомлення через WS.
  // Потрібно додати positions до контексту або підписатись окремо.
  // (Детально — Step 13 нижче)

  // ─── Рендер ─────────────────────────────────────────────────────────────
  const noSession = wsStatus === 'no-data' || session === null

  if (noSession) {
    return (
      <DataCard title="Live Track Map">
        <div className="flex flex-col items-center justify-center py-12 gap-3">
          <p className="text-text-secondary">No active session</p>
          <p className="text-text-secondary text-sm">
            Start F1 25 to automatically connect
          </p>
        </div>
      </DataCard>
    )
  }

  // ... решта рендеру
}
```

### 10.4 Як POSITIONS потрапляють у LiveTrackMap

`LiveTelemetryProvider` вже підписаний на `/topic/live/{sessionId}` і отримує POSITIONS повідомлення. Потрібно лише **прокинути їх у контекст**.

**Зміна в `LiveTelemetryProvider`:**

```typescript
// src/ws/LiveTelemetryProvider.tsx

// Додати state:
const [positions, setPositions] = useState<CarPositionDto[]>([])

// В обробнику WS повідомлень додати:
} else if (payload.type === 'POSITIONS') {
  setPositions((payload as WsPositionsMessage).positions ?? [])
}

// Додати в контекст:
export interface LiveTelemetryState {
  // ... існуючі поля ...
  positions: CarPositionDto[]   // ← NEW
}

// В useMemo:
positions,
```

**В `LiveTrackMap`:**

```typescript
const { session, status: wsStatus, positions } = useLiveTelemetry()
// positions автоматично оновлюються кожен раз як приходить POSITIONS з WS
// → React перерендерить болідbi на карті
```

### 10.5 Повна матриця переходів станів

```
WS status          session    layout    layoutStatus    → Що показуємо
─────────────────  ─────────  ────────  ──────────────  ──────────────────────────────
no-data            null       —         —               "No active session" placeholder
waiting            null       —         —               "Connecting..." (spinner)
live               {trackId}  loading   —               Skeleton / loading spinner
live               {trackId}  null      NOT_AVAILABLE   "Drive a lap to record layout"
live               {trackId}  null      RECORDING       Recording indicator + progress
live               {trackId}  данні     READY           TrackMap2D / TrackMap3D + болідb
disconnected       stale      данні     —               Трек + болідb (stale) + "Disconnected" badge
```

---

## 11. REST API Contract

### `GET /api/tracks/{trackId}/layout`

```json
Response 200:
{
  "trackId": 7,
  "points": [
    { "x": -234.5, "y": 3.2,  "z": 891.3 },
    { "x": -231.2, "y": 3.4,  "z": 895.1 },
    { "x": -190.0, "y": 15.8, "z": 920.0 }
  ],
  "bounds": {
    "minX": -450.0, "maxX": 350.0,
    "minZ": 600.0,  "maxZ": 1200.0,
    "minElev": 0.0, "maxElev": 28.5
  },
  "source": "RECORDED"
}

Нотатки:
  points[].x = worldPositionX (горизонталь ліво/право)
  points[].y = worldPositionY (elevation, метри над поверхнею)
  points[].z = worldPositionZ (горизонталь вперед/назад)
  bounds.minZ/maxZ = worldPositionZ bounds (НЕ elevation!)
  bounds.minElev/maxElev = worldPositionY bounds (elevation)
```

### `GET /api/tracks/{trackId}/layout/status` (новий)

```json
Response 200:
{
  "trackId": 7,
  "status": "RECORDING",
  "pointsCollected": 542,
  "source": null
}
```

---

## 12. Frontend залежності

```bash
npm install @react-three/fiber @react-three/drei three
npm install --save-dev @types/three
```

`@react-three/fiber` — React renderer для Three.js (найпопулярніший підхід).  
`@react-three/drei` — готові хелпери: `Line`, `OrbitControls`, `Text`, `Html`, `Grid`.

---

## 13. Покроковий план виконання (checklist)

### Phase 1 — Backend Core (6–8 годин)

- [ ] **1.1** — DB migration `23-track-layout-recording.sql`: колонки `min_elev, max_elev, source, recorded_at, session_uid, sector_boundaries` + UPDATE Silverstone мок (`{x,y}` → `{x,y,z}`, `sector_boundaries = null`)
- [ ] **1.2** — `PointXYZD` record (x, y, z, lapDistance) — замінює `PointXYZ`
- [ ] **1.3** — `TrackRecordingState`: буфер `List<PointXYZD>`, `addPoint(x,y,z,lapDistance)`
- [ ] **1.4** — `SessionRuntimeState`: додати `TrackRecordingState` + `getLatestLapDistance(carIndex)` + `sector2LapDistanceStart` + `sector3LapDistanceStart`
- [ ] **1.5** — `TrackLayoutEntity`: нові поля `minElev, maxElev, source, recordedAt, sessionUid, sectorBoundaries`
- [ ] **1.6** — `TrackLayoutRecordingService.onMotionFrame()`: передавати `lapDistance` → `addPoint(x, y, z, lapDistance)`
- [ ] **1.7** — `TrackLayoutRecordingService.saveTrackLayout()`: додати `buildSectorBoundaries(pts, s2Dist, s3Dist)` → зберігати в entity
- [ ] **1.8** — `SessionDataConsumer` / `SessionConsumer`: при отриманні `SessionDataDto` → зберігати `sector2/3LapDistanceStart` в `SessionRuntimeState`
- [ ] **1.9** — `MotionConsumer`: передавати `worldPositionY` + `lapDistance` в `onMotionFrame()`
- [ ] **1.10** — `LapDataProcessor`: виклик `onLapComplete()`
- [ ] **1.11** — `SessionLifecycleService` / `SessionConsumer`: виклики `onSessionStart()` + `onSessionFinished()`

### Phase 2 — Backend API (2–3 години)

- [ ] **2.1** — `TrackPointDto`: три поля `{x, y, z}`
- [ ] **2.2** — `BoundsDto`: додати `minElev, maxElev`
- [ ] **2.3** — `SectorBoundaryDto` record `{sector, x, y, z}`
- [ ] **2.4** — `TrackLayoutResponseDto`: оновити points + BoundsDto + source + `List<SectorBoundaryDto> sectorBoundaries`
- [ ] **2.5** — `TrackLayoutStatusDto` record
- [ ] **2.6** — `TrackLayoutService.getLayoutStatus(trackId)`
- [ ] **2.7** — `TrackController`: новий `GET /{trackId}/layout/status`
- [ ] **2.8** — REST contract doc: оновити § 3.5.4 (нові поля + sectorBoundaries) + § 3.5.5 (status)
- [ ] **2.9** — Unit tests: `TrackLayoutRecordingServiceTest` — всі сценарії + elevation bounds + `buildSectorBoundaries` (нормальний, s2/s3 fallback, порожній буфер)
- [ ] **2.10** — MockMvc tests: layout endpoint (перевірка `z` в points, `minElev` в bounds, `sectorBoundaries` масив з 3 елементами)

### Phase 3 — Frontend 2D + Sectors + Recording UI + Reactivity (5–6 годин)

- [ ] **3.1** — `src/api/types.ts`: `TrackPoint3D`, `TrackBounds`, `SectorBoundary`, оновити `TrackLayoutResponseDto` (додати `sectorBoundaries`) + `TrackLayoutStatusDto`, оновити `CarPositionDto` (додати `worldPosY`)
- [ ] **3.2** — `src/api/client.ts`: `getTrackLayoutStatus(trackId)`
- [ ] **3.3** — `src/utils/trackNormalization.ts`: `computeBounds`, `normalize2D`, `pointsToSvgPath`, `SECTOR_COLORS`, `findNearestPointIndex`, `splitIntoSectors`
- [ ] **3.4** — `TrackMap2D.tsx`: SVG з кольоровими секторами (3 `<path>`), ромб-маркери меж S2/S3, маркер Start/Finish, легенда
- [ ] **3.5** — `LiveTelemetryProvider`: додати `positions: CarPositionDto[]` в state + обробку `POSITIONS` WS повідомлення
- [ ] **3.6** — `LiveTrackMap`: **Ефект 1** — реакція на зміну `trackId` (скидати layout + запускати fetch/polling)
- [ ] **3.7** — `LiveTrackMap`: **Ефект 2** — polling `/layout/status` (2 сек RECORDING / 4 сек NOT_AVAILABLE) → автоматично fetch layout коли READY
- [ ] **3.8** — `LiveTrackMap`: Recording indicator UI (пульсуюча точка + прогрес-бар + текст)
- [ ] **3.9** — `LiveTrackMap`: стани "No active session" і "NOT_AVAILABLE"
- [ ] **3.10** — `LiveTrackMap`: читати `positions` з `useLiveTelemetry()` → передавати в `TrackMap2D`
- [ ] **3.11** — Manual test: сектори відображаються трьома різними кольорами, маркери S2/S3 на правильних місцях
- [ ] **3.12** — Manual test: без сесії → сесія з'являється → трек з секторами без перезавантаження

### Phase 4 — Frontend 3D + Sectors 3D (4–5 годин)

- [ ] **4.1** — `npm install @react-three/fiber @react-three/drei three @types/three`
- [ ] **4.2** — `trackNormalization.ts`: додати `computeThreeTransform`, `worldToThree`, `pointsToThreeBuffer`, `elevationToColor`
- [ ] **4.3** — `TrackMap3D.tsx`: три кольорових `<Line>` за секторами + `OrbitControls` + Sphere болідb
- [ ] **4.4** — `TrackMap3D.tsx`: 3D маркери меж секторів (Sphere + Html label) + Start/Finish маркер
- [ ] **4.5** — `LiveTrackMap`: toggle 2D/3D в header, підключити `TrackMap3D`
- [ ] **4.6** — Перевірити elevation + сектори у 3D (Spa або Austin)
- [ ] **4.7** — B9 positions: переконатись що `worldPosY` передається в `CarPositionDto`

### Phase 5 — Export / Import (2–3 години)

- [ ] **5.1** — `TrackLayoutExportDto` / `TrackLayoutBoundsExportDto` / `SectorBoundaryDto` в `telemetry-api-contracts`
- [ ] **5.2** — `TrackLayoutBulkExportDto` + `BulkImportResultDto` в `telemetry-api-contracts`
- [ ] **5.3** — `TrackLayoutService.exportLayout(trackId)` — single export
- [ ] **5.4** — `TrackLayoutService.exportAllLayouts()` — `findAll()` → sorted → `TrackLayoutBulkExportDto`
- [ ] **5.5** — `TrackController`: `GET /{trackId}/layout/export` (single) + `GET /layout/export-all` (bulk)
- [ ] **5.6** — `TrackLayoutService.importLayout(dto)` — single upsert via `save()`
- [ ] **5.7** — `TrackLayoutService.importAllLayouts(dto)` — loop + `importLayout()` per track + `BulkImportResultDto`
- [ ] **5.8** — `TrackController`: `POST /layout/import` (single) + `POST /layout/import-all` (bulk)
- [ ] **5.9** — Unit tests: `exportAllLayouts` (empty DB, 1 track, 3 tracks), `importAllLayouts` (happy path, частково невалідний)
- [ ] **5.10** — `src/api/client.ts`: `exportTrackLayout`, `exportAllTrackLayouts`, `importTrackLayout`, `importAllTrackLayouts`
- [ ] **5.11** — `LiveTrackMap`: Export + Export All кнопки в header (коли layout є)
- [ ] **5.12** — `LiveTrackMap`: Import + Import All у NOT_AVAILABLE стані; `handleImportAll` з fallback до single якщо файл без `tracks[]`
- [ ] **5.13** — Manual test E2E single: export one → clear DB → import → трек з секторами з'явився
- [ ] **5.14** — Manual test E2E bulk: записати 2 треки → export-all → clear DB → import-all → обидва треки відновлені

### Phase 6 — Documentation (1 година)

- [ ] **6.1** — `BACKEND_FEATURES_FOR_NEW_UI.md`: оновити B8 + B9
- [ ] **6.2** — `block-f-live-track-map.md`: розділ про auto-recording, 2D/3D, export/import
- [ ] **6.3** — REST contract doc: нові endpoint `GET /export` та `POST /import`

---

## 14. Залежності між кроками

```
1.1 → 1.5 → 1.7
1.2 → 1.3 → 1.6 → 1.9
1.4 → 1.6 → 1.10
           → 1.11
1.8 → 1.4 (sector distances треба до saveTrackLayout)
1.7 → 2.3 → 2.4
1.5 → 2.1 → 2.2 → 2.3
2.3 → 2.4 → 2.10
2.4 → 3.1 → 3.3 → 3.4 (2D sectors)
3.4 → 4.3 → 4.4 (3D sectors)
2.4 → 5.1 → 5.3 → 5.5 (single export)
     5.2 → 5.4 → 5.5 (bulk export — findAll, той самий контролер)
     5.1 → 5.6 → 5.8 (single import)
     5.2 → 5.7 → 5.8 (bulk import — loop single import)
     5.10 → 5.11 → 5.12 (frontend — після Phase 3)
Phase 1+2 → Phase 3 → Phase 4
Phase 2   → Phase 5
Phase 3   → Phase 5 (frontend частина)
```

---

## 15. Ризики і мітигація

| Ризик | Ймовірність | Мітигація |
|-------|-------------|-----------|
| Flashback → дублікати точок у неправильному порядку | Середня | Фільтр по `m_overallFrameIdentifier` (не відмотується) |
| Перший круг — виїзд з піт-лейну (неповне коло) | Висока | Детект `driverStatus = ON_TRACK (4)` перед початком запису |
| worldPositionY — дрібний шум (±0.1м на нерівностях) | Низька | Це норма, elevation коректний |
| Monaco: перепад висот ~2м — 3D майже плоский | Низька | Scale коректний; elevation info показуємо під canvas |
| Spa: перепад ~100м — трек "вертикальний" | Низька | scale = 100/maxHRange(XZ) — Y пропорційно масштабується |
| Breaking change `{x,y}` → `{x,y,z}` | Висока | Міграція 23 оновлює Silverstone мок; тест-ассерт на `z` поле |
| Three.js performance на ~1080 точок | Дуже низька | 1080 × 3 float = 12 KB — тривіально для WebGL |
| `@react-three/fiber` конфлікт версій з React | Низька | fiber v8 підтримує React 18; перевірити peer deps перед install |
| `sector2/3LapDistanceStart` = 0 або -1 (не отримали SessionData) | Середня | Fallback: `s2 = buffer.size/2`, `s3 = buffer.size*2/3`; логувати warning |
| Межа сектора знайдена неточно (decimation кожні 5 фреймів → ~4м похибка) | Низька | Допустимо для візуалізації; маркер буде в межах ±4м від реальної межі |
| `splitIntoSectors`: s2Idx > s3Idx (data anomaly) | Дуже низька | Swap + логувати; fallback до рівномірного розбиття на 3 частини |
| Відсутній `sectorBoundaries` (старі треки / Silverstone мок) | Висока | Перевірка `layout.sectorBoundaries?.length > 0`; якщо null → малювати весь трек одним кольором без маркерів |
| Import файлу зі старим форматом `{x,y}` (до v3/v4) | Середня | Перевірити `exportVersion`; якщо відсутній або `< 1` → відхилити з повідомленням "unsupported format" |
| Import на неіснуючий trackId (наприклад trackId=999) | Низька | JPA `save()` вставить рядок — це нормально; трек просто не матиме сесії і не відобразиться |
| Браузер блокує `window.open` для download | Дуже низька | Альтернатива: fetch blob + `URL.createObjectURL` + клік на `<a>` — стандартна техніка |
| Bulk import: один невалідний трек зупиняє всі | Низька | `try/catch` per track у `importAllLayouts` — помилка одного не зупиняє решту; всі помилки збираються в `errors[]` |
| Bulk export-all: дуже великий файл (10+ треків × 1080 точок) | Дуже низька | ~10 треків × ~80 KB ≈ 800 KB — цілком прийнятно для download |

---

---

## 16. Тестовий сценарій E2E

```
─── Сценарій A: Новий трек, запис і відображення ────────────────────────────

1. F1 25 не запущена. Відкрити Live Track Map.
   → "No active session. Start F1 25 to continue."
   → Видна кнопка "Import track JSON"

2. Запустити F1 25 на треку X (Spa або Austin).
   → Без перезавантаження: Recording indicator з прогрес-баром

3. Проїхати валідне коло
   → Трек з трьома кольорами секторів з'явився автоматично
   → В header — кнопки Export + toggle 2D/3D

─── Сценарій B: Bulk Export → скинути БД → Bulk Import ──────────────────────

4. Записати треки на 2+ трасах (Spa, Austin, Monaco).

5. Натиснути "Export All"
   → Браузер завантажує all-tracks-layout.json
   → Файл містить: { count: 3, tracks: [ {trackId:7, ...}, {trackId:15, ...}, ... ] }

6. Зупинити стек. Перезапустити з чистою БД.

7. Натиснути "Import All Tracks", обрати all-tracks-layout.json
   → POST /api/tracks/layout/import-all → { imported: 3, skipped: 0, errors: [] }
   → Toast: "Imported 3 track layouts"

8. Запустити F1 25 на будь-якому з трьох треків
   → Трек з секторами з'являється миттєво — жодного запису кола не потрібно

8. Запустити F1 25 на тому самому треку
   → WS підключається → Recording mode НЕ активується (трек вже є)
   → Болідb відразу відображаються на треку

─── Сценарій C: Import з іншим trackId ─────────────────────────────────────

9. Спробувати import файлу від треку 5 (Monaco) поки сесія на треку 7
   → Warning toast: "Importing track 5 but current session is on track 7"
   → Дані збережені в БД для trackId 5
   → Поточна карта НЕ змінюється (trackId сесії = 7)

─── Сценарій D: Верифікація точності секторів ───────────────────────────────

10. На Spa: переконатись що S1 (зелений) — від старту до ліса,
    S2 (жовтий) — від Eau Rouge до арени, S3 (пурпурний) — від Bus Stop до фінішу
    → Похибка маркерів ≤ ~5м
```

---

*Кінець плану v6. Наступний крок: Phase 1, Step 1.1 — DB migration.*

```
─── Сценарій A: Новий трек, користувач зайшов ДО запуску гри ───────────────

1. F1 25 не запущена. Відкрити Live Track Map.
   → "No active session. Start F1 25 to continue."

2. Запустити F1 25 на треку X (Spa або Austin).
   → Без перезавантаження:
      - З'являється Recording indicator: пульсуюча червона точка
      - "Drive one full lap to generate the track map"

3. Виїхати на трасу → прогрес-бар зростає (0 → 300+)

4. Проїхати валідне коло → перетнути стартову лінію
   → Без перезавантаження:
      - Recording indicator зникає
      - [2D] Трек відмалюється трьома кольорами:
          Зелений — Sector 1
          Жовтий  — Sector 2
          Пурпурний — Sector 3
      - На межах секторів: ромб-маркери "S2", "S3"
      - На старті/фініші: маркер "S/F"
      - Легенда у верхньому лівому куті

─── Сценарій B: Трек вже є в БД (перезахід) ────────────────────────────────

5. Перезайти на сторінку (F1 25 продовжує їхати)
   → Трек з секторами завантажується миттєво
   → source: "RECORDED" в DevTools Network

6. Перемкнути на [3D]
   → Три кольорових відрізки треку (S1/S2/S3)
   → Маркери S2 і S3 як сфери з Html підписами
   → Start/Finish маркер (білий стовпчик)
   → Болід рухається по 3D треку
   → OrbitControls: обертати, зумити, панорамувати

─── Сценарій C: Верифікація точності меж секторів ───────────────────────────

7. Порівняти позицію маркерів з реальним треком:
   → Межа S1→S2 та S2→S3 мають бути в межах ±5м від реальної межі
   → Допустима похибка через decimation (1 точка кожні ~4-5м при 12 Гц і 60 км/год)

─── Сценарій D: Трек без sector_boundaries (Silverstone мок) ────────────────

8. Для Silverstone (static мок без sectorBoundaries):
   → Трек малюється одним кольором (без розбивки на сектори)
   → Маркери НЕ відображаються
   → Немає помилок в консолі

─── Сценарій E: Сесія завершилась та знову почалась ─────────────────────────

9. SESSION_ENDED → трек і болідb залишаються (stale) + "Disconnected" badge
10. Нова сесія на тому ж треку → автопідключення, позиції оновлюються
    → Recording mode НЕ активується
```

---

*Кінець плану v6. Наступний крок: Phase 1, Step 1.1 — DB migration.*
