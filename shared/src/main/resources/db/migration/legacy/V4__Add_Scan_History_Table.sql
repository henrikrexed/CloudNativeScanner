-- Create scan_history table
CREATE TABLE scan_history (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources(id),
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    topics_found INTEGER DEFAULT 0,
    topics_processed INTEGER DEFAULT 0,
    topics_new INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'RUNNING',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_scan_history_source_id ON scan_history(source_id);
CREATE INDEX idx_scan_history_started_at ON scan_history(started_at);
CREATE INDEX idx_scan_history_status ON scan_history(status);











