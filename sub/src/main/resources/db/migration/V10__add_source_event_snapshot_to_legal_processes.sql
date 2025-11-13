ALTER TABLE legal_processes
    ADD COLUMN IF NOT EXISTS source_event_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_event_snapshot TEXT;
