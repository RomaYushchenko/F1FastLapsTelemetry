-- Етап 2.11: Retention policy для raw таблиць (опційно)
-- Джерело: f_1_telemetry_project_architecture.md § 9.7.1

SELECT add_retention_policy('telemetry.car_telemetry_raw', INTERVAL '14 days', if_not_exists => TRUE);
SELECT add_retention_policy('telemetry.car_status_raw', INTERVAL '14 days', if_not_exists => TRUE);
