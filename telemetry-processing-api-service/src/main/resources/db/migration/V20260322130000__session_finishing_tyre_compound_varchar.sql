-- Hibernate maps @Column(length=1) on String to VARCHAR(1); PostgreSQL CHAR(1) is JDBC type CHAR (bpchar) and fails ddl validate.
ALTER TABLE telemetry.session_finishing_positions
  ALTER COLUMN tyre_compound TYPE VARCHAR(1)
  USING RTRIM(tyre_compound::text);
