CREATE TABLE devices(
  id             BIGSERIAL         PRIMARY KEY,
  name           text           NOT NULL,
  resin_name     text           NOT NULL,
  short_link     text           NOT NULL,
  created_on     TIMESTAMP      NOT NULL
);
