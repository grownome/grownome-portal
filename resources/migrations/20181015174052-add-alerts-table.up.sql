CREATE TABLE alerts (
  id           BIGSERIAL        PRIMARY KEY,
  device_id    BIGINT           REFERENCES devices(id),
  created_on   TIMESTAMP        NOT NULL,
  user_id      VARCHAR(50)      REFERENCES users(id),
  metric_name  VARCHAR(50)      NOT NULL,
  threshold    INT              NOT NULL,
  above        BOOLEAN          NOT NULL,
  below        BOOLEAN          NOT NULL,
  phone_number VARCHAR(20)      NOT NULL,
  description  TEXT
);
