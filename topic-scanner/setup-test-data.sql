-- Setup script to create test data for debugging the topic scanner
-- This script creates the necessary database structure and test data

-- Create database (run this separately if needed)
-- CREATE DATABASE cloud_native_scanner;

-- Connect to the database and create tables
\c cloud_native_scanner;

-- Create themes table
CREATE TABLE IF NOT EXISTS themes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create sources table
CREATE TABLE IF NOT EXISTS sources (
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
CREATE TABLE IF NOT EXISTS topics (
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
CREATE TABLE IF NOT EXISTS topic_themes (
    topic_id BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    theme_id BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    confidence_score DECIMAL(3,2) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (topic_id, theme_id)
);

-- Create scan_history table for tracking scanning activities
CREATE TABLE IF NOT EXISTS scan_history (
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

-- Create search_topics table
CREATE TABLE IF NOT EXISTS search_topics (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    keyword VARCHAR(255) NOT NULL,
    search_query VARCHAR(500),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 1, -- 1=high, 2=medium, 3=low
    max_results INTEGER DEFAULT 50,
    search_frequency_hours INTEGER DEFAULT 24,
    last_searched_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_topics_source_id ON topics(source_id);
CREATE INDEX IF NOT EXISTS idx_topics_external_id ON topics(external_id);
CREATE INDEX IF NOT EXISTS idx_topics_created_at ON topics(created_at);
CREATE INDEX IF NOT EXISTS idx_topics_last_scanned_at ON topics(last_scanned_at);
CREATE INDEX IF NOT EXISTS idx_topic_themes_topic_id ON topic_themes(topic_id);
CREATE INDEX IF NOT EXISTS idx_topic_themes_theme_id ON topic_themes(theme_id);
CREATE INDEX IF NOT EXISTS idx_scan_history_source_id ON scan_history(source_id);
CREATE INDEX IF NOT EXISTS idx_scan_history_started_at ON scan_history(started_at);
CREATE INDEX IF NOT EXISTS idx_search_topics_source_id ON search_topics(source_id);
CREATE INDEX IF NOT EXISTS idx_search_topics_keyword ON search_topics(keyword);
CREATE INDEX IF NOT EXISTS idx_search_topics_is_active ON search_topics(is_active);
CREATE INDEX IF NOT EXISTS idx_search_topics_priority ON search_topics(priority);
CREATE INDEX IF NOT EXISTS idx_search_topics_last_searched_at ON search_topics(last_searched_at);

-- Clear existing data
DELETE FROM search_topics;
DELETE FROM scan_history;
DELETE FROM topic_themes;
DELETE FROM topics;
DELETE FROM themes;
DELETE FROM sources;

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

-- Insert search topics for StackOverflow
INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'kubernetes', 'kubernetes', 'Kubernetes-related questions and discussions', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'docker', 'docker', 'Docker containerization questions', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'microservices', 'microservices', 'Microservices architecture discussions', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'cloud-native', 'cloud native', 'Cloud-native development topics', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'spring-boot', 'spring boot', 'Spring Boot framework questions', 2, 30
FROM sources s WHERE s.name = 'StackOverflow';

-- Insert search topics for Reddit
INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'kubernetes', 'kubernetes', 'Kubernetes discussions on Reddit', 1, 50
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'docker', 'docker', 'Docker discussions on Reddit', 1, 50
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'devops', 'devops', 'DevOps discussions on Reddit', 1, 50
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'cloud-native', 'cloud native', 'Cloud-native discussions on Reddit', 1, 50
FROM sources s WHERE s.name = 'Reddit';

-- Show the data we just created
SELECT 'Sources:' as info;
SELECT id, name, base_url, is_active FROM sources;

SELECT 'Search Topics:' as info;
SELECT st.id, s.name as source, st.keyword, st.search_query, st.is_active, st.priority 
FROM search_topics st 
JOIN sources s ON st.source_id = s.id 
ORDER BY s.name, st.priority;

SELECT 'Themes:' as info;
SELECT id, name, description FROM themes;

-- Show counts
SELECT 'Data Summary:' as info;
SELECT 
    (SELECT COUNT(*) FROM sources) as sources_count,
    (SELECT COUNT(*) FROM search_topics) as search_topics_count,
    (SELECT COUNT(*) FROM themes) as themes_count;

