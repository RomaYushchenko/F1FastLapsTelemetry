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
- `05-car-telemetry-raw.sql` — `car_telemetry_raw` (hypertable)
- `06-car-status-raw.sql` — `car_status_raw` (hypertable)
- `07-laps.sql` — `laps`
- `08-session-summary.sql` — `session_summary`
- `09-retention.sql` — retention policy 14 днів для raw
- `10-sessions-public-id.sql` — public_id для sessions (якщо є)
- `11-pedal-trace.sql` — колонки `lap_number`, `lap_distance_m` у `car_telemetry_raw` для pedal trace

Документація DDL: [f_1_telemetry_project_architecture.md](../.github/project/f_1_telemetry_project_architecture.md) § 9.
