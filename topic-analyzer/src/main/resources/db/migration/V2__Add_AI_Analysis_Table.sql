x-- Add AI analysis table for enhanced topic understanding
CREATE TABLE topic_analysis (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    ai_summary TEXT,
    complexity_level VARCHAR(50),
    relevance_score DECIMAL(3,2),
    keywords TEXT, -- JSON array of keywords
    ai_confidence DECIMAL(3,2),
    analysis_version VARCHAR(20) DEFAULT '1.0',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(topic_id)
);

-- Create indexes for better performance
CREATE INDEX idx_topic_analysis_topic_id ON topic_analysis(topic_id);
CREATE INDEX idx_topic_analysis_relevance_score ON topic_analysis(relevance_score);
CREATE INDEX idx_topic_analysis_complexity_level ON topic_analysis(complexity_level);
CREATE INDEX idx_topic_analysis_created_at ON topic_analysis(created_at);

-- Add AI configuration table
CREATE TABLE ai_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
        description TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- Insert default AI configuration
    INSERT INTO ai_config (config_key, config_value, description) VALUES 
    ('ai_enabled', 'true', 'Enable AI-powered topic analysis'),
    ('ai_similarity_threshold', '0.8', 'Threshold for semantic similarity detection'),
    ('ai_confidence_threshold', '0.7', 'Minimum confidence score for AI analysis'),
('ai_model_version', 'gpt-3.5-turbo', 'AI model version to use'),
('ai_max_tokens', '1000', 'Maximum tokens for AI analysis'),
('ai_temperature', '0.3', 'AI model temperature for analysis');
