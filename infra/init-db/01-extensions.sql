-- Етап 2.2: TimescaleDB та опційні розширення
-- Джерело: f_1_telemetry_project_architecture.md § 9.1

CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
