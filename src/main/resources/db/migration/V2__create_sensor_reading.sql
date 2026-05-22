CREATE TABLE sensor_reading (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    sensor_id      TEXT             NOT NULL,
    location       TEXT             NOT NULL,
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    metric         TEXT             NOT NULL,
    unit           TEXT             NOT NULL,
    value          DOUBLE PRECISION,
    smoothed_value DOUBLE PRECISION,
    observed_at    TIMESTAMPTZ      NOT NULL,
    ingested_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    source         TEXT             NOT NULL DEFAULT 'open-meteo',
    schema_version INT              NOT NULL DEFAULT 1,
    quality_status TEXT             NOT NULL DEFAULT 'OK',
    PRIMARY KEY (id, observed_at),
    UNIQUE (sensor_id, observed_at)
);

SELECT create_hypertable('sensor_reading', 'observed_at', if_not_exists => TRUE);

CREATE INDEX idx_reading_loc_metric_time ON sensor_reading (location, metric, observed_at DESC);
