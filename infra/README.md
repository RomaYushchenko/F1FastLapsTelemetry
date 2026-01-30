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

Після старту Kafka створіть topics (одноразово або після чистого volume):

```bash
chmod +x scripts/create-kafka-topics.sh
./scripts/create-kafka-topics.sh
```

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

Документація DDL: [f_1_telemetry_project_architecture.md](../.github/project/f_1_telemetry_project_architecture.md) § 9.
