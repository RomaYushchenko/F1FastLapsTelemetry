# Infra

Docker Compose та скрипти для локального запуску (Етап 2, implementation_steps_plan).

## Склад

- **Zookeeper** — порт 2181, volume `zookeeper-data`
- **Kafka** — порт 9092, volume `kafka-data`
- **PostgreSQL + TimescaleDB** — порт 5432, volume `postgres-data`, init-скрипти в `init-db/`

## Запуск

```bash
cd infra
docker-compose up -d
```

**Kafka topics:** Топіки створюються автоматично брокером при першій публікації (`KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"` у docker-compose). Скрипт `scripts/create-kafka-topics.sh` опціональний (наприклад, якщо потрібні топіки до першого запуску сервісів або інші параметри).

## Підключення

| Сервіс   | URL / параметри |
|----------|------------------|
| Kafka    | `localhost:9092` |
| PostgreSQL | host=localhost, port=5432, db=telemetry, user=telemetry, password=telemetry |

## Init DB

При першому запуску контейнера Postgres виконуються скрипти з `init-db/` по алфавіту:

- `01-extensions.sql` — TimescaleDB, pgcrypto
- `02-schema.sql` — схема `telemetry`
- `03-sessions.sql` — `sessions`, `session_cars`
- `04-processed-packets.sql` — `processed_packets`
- `04-add-player-car-index.sql` — колонка `player_car_index` у `telemetry.sessions`
- `05-car-telemetry-raw.sql` — `car_telemetry_raw` (hypertable)
- `06-car-status-raw.sql` — `car_status_raw` (hypertable)
- `07-laps.sql` — `laps`
- `08-session-summary.sql` — `session_summary`
- `09-retention.sql` — retention policy 14 днів для raw
- `10-sessions-public-id.sql` — public_id для sessions (якщо є)
- `11-pedal-trace.sql` — колонки `lap_number`, `lap_distance_m` у `car_telemetry_raw` для pedal trace
- `12-tyre-wear-per-lap.sql` — таблиця `tyre_wear_per_lap`
- `13-car-status-tyres-age-laps.sql` — колонка `tyres_age_laps` у `car_status_raw`
- `14-session-display-name.sql` — колонка `session_display_name` у `telemetry.sessions`
- `15-session-finishing-positions.sql` — таблиця `session_finishing_positions`
- `16-tyre-wear-per-lap-compound.sql` — колонка `compound` у `telemetry.tyre_wear_per_lap`
- `17-laps-position-at-lap-start.sql` — колонка `position_at_lap_start` у `telemetry.laps`
- `20-session-drivers.sql` — таблиця `session_drivers`
- `21-session-events.sql` — таблиця `session_events`
- `22-track-layout.sql` — таблиця `track_layout`
- `23-track-layout-recording.sql` — колонки recording для `track_layout`
- `24-migrate-all-static-tracks.sql` — міграція static track layouts
- `25-drop-motion-and-corner-tables.sql` — **не для порожньої БД**: одноразове видалення legacy-таблиць `lap_corner_metrics`, `track_corners`, `track_corner_maps`, `motion_raw` на існуючих інстансах (раніше створювались скриптами 18/19, які прибрані з репозиторію)

Документація DDL: [f_1_telemetry_project_architecture.md](../.github/project/f_1_telemetry_project_architecture.md) § 9.

### Existing database (migrations)

Init scripts run **only on first Postgres start** (empty data dir). If the DB was created before some scripts were added, apply the missing ones manually (in numeric order). Scripts to add for an older DB (run in numeric order; all use IF NOT EXISTS / idempotent):

- `04-add-player-car-index.sql` — column `player_car_index` on `telemetry.sessions`
- `12-tyre-wear-per-lap.sql` — table `telemetry.tyre_wear_per_lap` (required before 16)
- `13-car-status-tyres-age-laps.sql` — column `tyres_age_laps` on `telemetry.car_status_raw`
- `14-session-display-name.sql` — column `session_display_name` on `telemetry.sessions`
- `15-session-finishing-positions.sql` — table `telemetry.session_finishing_positions`
- `16-tyre-wear-per-lap-compound.sql` — column `compound` on `telemetry.tyre_wear_per_lap`
- `17-laps-position-at-lap-start.sql` — column `position_at_lap_start` on `telemetry.laps`

**Dropping legacy corner / motion_raw tables (existing DBs only):**

- `25-drop-motion-and-corner-tables.sql` — drops `lap_corner_metrics`, `track_corners`, `track_corner_maps`, `motion_raw` if present. Run once when upgrading from a DB that had scripts 18/19.

**Docker (from project root) — run all migration scripts:**

```bash
docker exec -i f1-telemetry-postgres psql -U telemetry -d telemetry < infra/init-db/04-add-player-car-index.sql
docker exec -i f1-telemetry-postgres psql -U telemetry -d telemetry < infra/init-db/12-tyre-wear-per-lap.sql
docker exec -i f1-telemetry-postgres psql -U telemetry -d telemetry < infra/init-db/13-car-status-tyres-age-laps.sql
docker exec -i f1-telemetry-postgres psql -U telemetry -d telemetry < infra/init-db/14-session-display-name.sql
docker exec -i f1-telemetry-postgres psql -U telemetry -d telemetry < infra/init-db/15-session-finishing-positions.sql
docker exec -i f1-telemetry-postgres psql -U telemetry -d telemetry < infra/init-db/16-tyre-wear-per-lap-compound.sql
docker exec -i f1-telemetry-postgres psql -U telemetry -d telemetry < infra/init-db/17-laps-position-at-lap-start.sql
# If upgrading from a DB that had corner/motion_raw tables:
# docker exec -i f1-telemetry-postgres psql -U telemetry -d telemetry < infra/init-db/25-drop-motion-and-corner-tables.sql
```

**Local psql — run all migration scripts:**

```bash
psql -h localhost -p 5432 -U telemetry -d telemetry -f infra/init-db/04-add-player-car-index.sql
psql -h localhost -p 5432 -U telemetry -d telemetry -f infra/init-db/12-tyre-wear-per-lap.sql
psql -h localhost -p 5432 -U telemetry -d telemetry -f infra/init-db/13-car-status-tyres-age-laps.sql
psql -h localhost -p 5432 -U telemetry -d telemetry -f infra/init-db/14-session-display-name.sql
psql -h localhost -p 5432 -U telemetry -d telemetry -f infra/init-db/15-session-finishing-positions.sql
psql -h localhost -p 5432 -U telemetry -d telemetry -f infra/init-db/16-tyre-wear-per-lap-compound.sql
psql -h localhost -p 5432 -U telemetry -d telemetry -f infra/init-db/17-laps-position-at-lap-start.sql
# If upgrading from a DB that had corner/motion_raw tables:
# psql -h localhost -p 5432 -U telemetry -d telemetry -f infra/init-db/25-drop-motion-and-corner-tables.sql
```
