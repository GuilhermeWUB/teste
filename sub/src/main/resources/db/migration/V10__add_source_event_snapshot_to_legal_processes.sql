ALTER TABLE legal_processes
    ADD COLUMN source_event_id BIGINT,
    ADD COLUMN source_event_snapshot TEXT;
