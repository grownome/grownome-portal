CREATE TABLE images(
  md5              UUID           PRIMARY KEY,
  device_id        BIGSERIAL      REFERENCES devices(id),
  path             VARCHAR (355)  NOT NULL,
  created_on       TIMESTAMP      NOT NULL
);
