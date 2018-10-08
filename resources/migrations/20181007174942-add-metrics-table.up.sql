CREATE TABLE metrics(
  device_registry_id	      VARCHAR(50) NOT NULL,
  device_id	                INTEGER     REFERENCES devices(id),
  core_temp_max	            NUMERIC,
  core_temp_main	          NUMERIC,
  humidity	                NUMERIC,
  dewpoint                  NUMERIC,
  temperature	              NUMERIC,
  timestamp	                TIMESTAMP   NOT NULL
);
