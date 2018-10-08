CREATE TABLE owners(
  id                  SERIAL         PRIMARY KEY,
  user_id             VARCHAR (50)   REFERENCES users(id),
  device_id           INTEGER        REFERENCES devices(id),
  created_on          TIMESTAMP      NOT NULL
);
