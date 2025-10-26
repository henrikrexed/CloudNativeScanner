-- Create themes table
CREATE TABLE themes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create sources table
CREATE TABLE sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    base_url VARCHAR(500) NOT NULL,
    api_endpoint VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    scan_frequency_hours INTEGER DEFAULT 24,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create topics table
CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources(id),
    external_id VARCHAR(255) NOT NULL,
    title TEXT NOT NULL,
    content TEXT,
    url VARCHAR(1000) NOT NULL,
    interaction_count INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    score INTEGER DEFAULT 0,
    author VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_scanned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(source_id, external_id)
);

-- Create topic_themes table (many-to-many relationship)
CREATE TABLE topic_themes (
    topic_id BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    theme_id BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    confidence_score DECIMAL(3,2) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (topic_id, theme_id)
);

-- Create scan_history table for tracking scanning activities
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
CREATE INDEX idx_topics_source_id ON topics(source_id);
CREATE INDEX idx_topics_external_id ON topics(external_id);
CREATE INDEX idx_topics_created_at ON topics(created_at);
CREATE INDEX idx_topics_last_scanned_at ON topics(last_scanned_at);
CREATE INDEX idx_topic_themes_topic_id ON topic_themes(topic_id);
CREATE INDEX idx_topic_themes_theme_id ON topic_themes(theme_id);
CREATE INDEX idx_scan_history_source_id ON scan_history(source_id);
CREATE INDEX idx_scan_history_started_at ON scan_history(started_at);

-- Insert default themes
INSERT INTO themes (name, description) VALUES 
('Cloud Native', 'Topics related to cloud-native technologies, containers, microservices'),
('Kubernetes', 'Kubernetes-specific discussions, deployments, and configurations'),
('DevOps', 'DevOps practices, CI/CD, automation, and infrastructure'),
('Security', 'Security-related topics, vulnerabilities, and best practices'),
('Monitoring', 'Observability, monitoring, logging, and alerting'),
('Development', 'General development topics, programming languages, frameworks'),
('Architecture', 'System design, architecture patterns, and scalability'),
('Performance', 'Performance optimization, tuning, and benchmarking');

-- Insert default sources
INSERT INTO sources (name, base_url, api_endpoint, scan_frequency_hours) VALUES 
('StackOverflow', 'https://stackoverflow.com', 'https://api.stackexchange.com/2.3', 24),
('Reddit', 'https://reddit.com', 'https://www.reddit.com/r', 24);

