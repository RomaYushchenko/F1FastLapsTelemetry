-- Session events: F1 Packet Event (3) persisted for timeline (FTLP, PENA, SCAR, etc.).
-- Block E — Session events. Order by (lap, frame_id).

CREATE TABLE IF NOT EXISTS telemetry.session_events (
  id          BIGSERIAL   NOT NULL,
  session_uid BIGINT      NOT NULL,
  frame_id    INTEGER     NOT NULL,
  lap         SMALLINT    NULL,
  event_code  VARCHAR(8)  NOT NULL,
  car_index   SMALLINT    NULL,
  detail      JSONB       NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT fk_session_events_session FOREIGN KEY (session_uid) REFERENCES telemetry.sessions(session_uid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_session_events_session_lap_frame
  ON telemetry.session_events(session_uid, lap, frame_id);
