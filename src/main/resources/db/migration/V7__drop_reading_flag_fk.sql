-- TimescaleDB hypertables do not support FK references from other tables.
ALTER TABLE reading_flag DROP CONSTRAINT reading_flag_reading_id_observed_at_fkey;
