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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create topic_themes table (many-to-many relationship between topics and themes)
CREATE TABLE topic_themes (
    topic_id BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    theme_id BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    confidence_score DECIMAL(3,2) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (topic_id, theme_id)
);

-- Create indexes for better performance
CREATE INDEX idx_topics_source_id ON topics(source_id);
CREATE INDEX idx_topics_created_at ON topics(created_at);
CREATE INDEX idx_topics_interaction_count ON topics(interaction_count);
CREATE INDEX idx_sources_is_active ON sources(is_active);

-- Insert default themes
INSERT INTO themes (name, description) VALUES
('Kubernetes', 'Topics related to Kubernetes and container orchestration'),
('Cloud Native', 'Topics related to cloud native development and architectures'),
('DevOps', 'Topics related to DevOps practices and tools'),
('Microservices', 'Topics related to microservices architecture'),
('Observability', 'Topics related to monitoring, logging, and tracing');

-- Insert default sources
INSERT INTO sources (name, base_url, api_endpoint, is_active, scan_frequency_hours) VALUES
('StackOverflow', 'https://stackoverflow.com', 'https://api.stackexchange.com/2.3', true, 24),
('Reddit', 'https://www.reddit.com', 'https://www.reddit.com', true, 24),
('YouTube', 'https://www.youtube.com', 'https://www.googleapis.com/youtube/v3', true, 2),
('DevTo', 'https://dev.to', 'https://dev.to/api', true, 1),
('Hashnode', 'https://hashnode.com', 'https://api.hashnode.com', true, 2),
('Medium', 'https://medium.com', 'https://medium.com/feed', true, 2);



