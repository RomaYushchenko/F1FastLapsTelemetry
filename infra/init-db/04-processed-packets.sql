-- Етап 2.6: processed_packets (idempotency)
-- Джерело: f_1_telemetry_project_architecture.md § 9.4

CREATE TABLE IF NOT EXISTS telemetry.processed_packets (
  session_uid      BIGINT      NOT NULL,
  frame_identifier INTEGER     NOT NULL,
  packet_id        SMALLINT    NOT NULL,
  car_index        SMALLINT    NOT NULL,

  processed_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (session_uid, frame_identifier, packet_id, car_index)
);

CREATE INDEX IF NOT EXISTS idx_processed_packets_time ON telemetry.processed_packets(processed_at DESC);
