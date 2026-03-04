-- TopicScanner v2.0 — Clean schema
-- This replaces all V1-V22 legacy migrations

CREATE EXTENSION IF NOT EXISTS vector;

-- Sources (data platforms: reddit, youtube, medium, etc.)
CREATE TABLE sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    base_url VARCHAR(500) NOT NULL,
    api_endpoint VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    scan_frequency_hours INTEGER DEFAULT 24,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Categories (user-defined topic categories that drive the pipeline)
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    keywords TEXT,
    negative_keywords TEXT,
    sources TEXT,
    scan_frequency VARCHAR(20) DEFAULT 'daily',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Topics (discovered content items)
CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources(id),
    category_id BIGINT REFERENCES categories(id),
    external_id VARCHAR(500) NOT NULL,
    title TEXT NOT NULL,
    content TEXT,
    extracted_content TEXT,
    url VARCHAR(2000) NOT NULL,
    author VARCHAR(200),
    interaction_count INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    score INTEGER DEFAULT 0,
    engagement_score NUMERIC(5,2) DEFAULT 0,

    -- Pipeline status
    pipeline_stage VARCHAR(20) DEFAULT 'scanned',
    extraction_status VARCHAR(20) DEFAULT 'PENDING',

    -- Analysis results
    language VARCHAR(10),
    relevance_score FLOAT,
    relevance_reasoning TEXT,
    quality_score FLOAT,
    summary TEXT,
    tags TEXT,
    group_id VARCHAR(100),
    embedding vector(1536),

    -- Filtering
    is_rejected BOOLEAN DEFAULT false,
    rejection_reason TEXT,

    -- Timestamps
    source_date TIMESTAMP,
    collected_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_scanned_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT uq_source_external UNIQUE (source_id, external_id)
);

CREATE INDEX idx_topics_source ON topics(source_id);
CREATE INDEX idx_topics_category ON topics(category_id);
CREATE INDEX idx_topics_pipeline_stage ON topics(pipeline_stage);
CREATE INDEX idx_topics_extraction_status ON topics(extraction_status);
CREATE INDEX idx_topics_group_id ON topics(group_id);
CREATE INDEX idx_topics_created_at ON topics(created_at DESC);

-- Pipeline job queue (replaces Kafka)
CREATE TABLE pipeline_jobs (
    id BIGSERIAL PRIMARY KEY,
    stage VARCHAR(20) NOT NULL,
    topic_id BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    claimed_at TIMESTAMP,
    completed_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_pipeline_jobs_claimable
    ON pipeline_jobs (stage, status, created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_pipeline_jobs_stage_status
    ON pipeline_jobs (stage, status);

CREATE INDEX idx_pipeline_jobs_topic_id
    ON pipeline_jobs (topic_id);

-- User content (uploaded for writing style learning)
CREATE TABLE user_content (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500),
    content TEXT NOT NULL,
    content_type VARCHAR(50),
    writing_style_metadata TEXT,
    embedding vector(1536),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Generated content drafts
CREATE TABLE generated_content (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT REFERENCES topics(id),
    group_id VARCHAR(100),
    output_format VARCHAR(50),
    generated_text TEXT NOT NULL,
    prompt_used TEXT,
    model_used VARCHAR(100),
    feedback TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Scan history (monitoring)
CREATE TABLE scan_history (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(50) NOT NULL,
    category_id BIGINT REFERENCES categories(id),
    topics_found INTEGER DEFAULT 0,
    topics_new INTEGER DEFAULT 0,
    errors INTEGER DEFAULT 0,
    error_details TEXT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_scan_history_source ON scan_history(source, started_at DESC);

-- Seed default sources
INSERT INTO sources (name, base_url, api_endpoint) VALUES
    ('Reddit', 'https://www.reddit.com', 'https://www.reddit.com'),
    ('StackOverflow', 'https://stackoverflow.com', 'https://api.stackexchange.com/2.3'),
    ('Medium', 'https://medium.com', 'https://medium.com'),
    ('Dev.to', 'https://dev.to', 'https://dev.to/api'),
    ('Hashnode', 'https://hashnode.com', 'https://gql.hashnode.com'),
    ('YouTube', 'https://www.youtube.com', 'https://www.googleapis.com/youtube/v3'),
    ('GitHub', 'https://github.com', 'https://api.github.com');
