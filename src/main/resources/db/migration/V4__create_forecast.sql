CREATE TABLE forecast (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location    TEXT             NOT NULL,
    metric      TEXT             NOT NULL,
    horizon_at  TIMESTAMPTZ      NOT NULL,
    predicted   DOUBLE PRECISION NOT NULL,
    lower_bound DOUBLE PRECISION NOT NULL,
    upper_bound DOUBLE PRECISION NOT NULL,
    model       TEXT             NOT NULL,
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT now(),
    UNIQUE (location, metric, horizon_at)
);

CREATE INDEX idx_forecast_loc_metric_time ON forecast (location, metric, horizon_at DESC);
