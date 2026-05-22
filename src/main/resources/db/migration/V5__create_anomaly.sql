CREATE TABLE anomaly (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sensor_id   TEXT             NOT NULL,
    location    TEXT             NOT NULL,
    metric      TEXT             NOT NULL,
    observed_at TIMESTAMPTZ      NOT NULL,
    value       DOUBLE PRECISION,
    method      TEXT             NOT NULL,
    score       DOUBLE PRECISION NOT NULL,
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE INDEX idx_anomaly_loc_metric_time ON anomaly (location, metric, observed_at DESC);
