CREATE TABLE crm_activity (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    status VARCHAR(40) NOT NULL,
    type VARCHAR(60) NOT NULL,
    lead_source VARCHAR(120),
    responsible VARCHAR(120),
    city VARCHAR(120),
    state VARCHAR(16),
    due_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crm_activity_status ON crm_activity(status);
CREATE INDEX idx_crm_activity_due_at ON crm_activity(due_at);
