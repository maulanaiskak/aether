CREATE TABLE forecast_metrics (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location     TEXT             NOT NULL,
    metric       TEXT             NOT NULL,
    model        TEXT             NOT NULL,
    mae          DOUBLE PRECISION NOT NULL,
    rmse         DOUBLE PRECISION NOT NULL,
    evaluated_at TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE INDEX idx_forecast_metrics_loc_model_time ON forecast_metrics (location, model, evaluated_at DESC);
