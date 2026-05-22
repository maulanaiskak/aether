CREATE TABLE reading_flag (
    reading_id  BIGINT      NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    flag        TEXT        NOT NULL,
    PRIMARY KEY (reading_id, observed_at, flag),
    FOREIGN KEY (reading_id, observed_at) REFERENCES sensor_reading (id, observed_at) ON DELETE CASCADE
);

CREATE INDEX idx_reading_flag_flag ON reading_flag (flag);
