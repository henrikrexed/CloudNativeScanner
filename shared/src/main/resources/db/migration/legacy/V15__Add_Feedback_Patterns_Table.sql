-- Create feedback_patterns table to store learned patterns from user feedback
-- This enables scanners to improve their search behavior based on feedback

CREATE TABLE IF NOT EXISTS feedback_patterns (
    id BIGSERIAL PRIMARY KEY,
    pattern_type VARCHAR(50) NOT NULL, -- 'AVOID' or 'PRIORITIZE'
    pattern_text TEXT NOT NULL, -- The keyword, phrase, or pattern
    pattern_category VARCHAR(100), -- 'KEYWORD', 'PHRASE', 'TITLE_PATTERN', 'CONTENT_PATTERN'
    source_id BIGINT, -- Optional: pattern specific to a source
    theme_id BIGINT, -- Optional: pattern specific to a theme
    rejection_reason TEXT, -- For AVOID patterns: why it was rejected
    confidence_score DECIMAL(3,2) DEFAULT 1.0, -- How confident we are in this pattern (0.0-1.0)
    usage_count INTEGER DEFAULT 0, -- How many times this pattern has been used
    success_count INTEGER DEFAULT 0, -- How many times filtering by this pattern prevented bad topics
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_id) REFERENCES sources(id) ON DELETE CASCADE,
    FOREIGN KEY (theme_id) REFERENCES themes(id) ON DELETE CASCADE
);

-- Indexes for efficient pattern lookup
CREATE INDEX IF NOT EXISTS idx_feedback_patterns_type ON feedback_patterns(pattern_type);
CREATE INDEX IF NOT EXISTS idx_feedback_patterns_category ON feedback_patterns(pattern_category);
CREATE INDEX IF NOT EXISTS idx_feedback_patterns_source ON feedback_patterns(source_id);
CREATE INDEX IF NOT EXISTS idx_feedback_patterns_theme ON feedback_patterns(theme_id);
CREATE INDEX IF NOT EXISTS idx_feedback_patterns_text ON feedback_patterns USING gin(to_tsvector('english', pattern_text));

-- Add comments
COMMENT ON TABLE feedback_patterns IS 'Stores learned patterns from user feedback to improve scanner quality. Patterns can be keywords, phrases, or content patterns that should be avoided or prioritized.';
COMMENT ON COLUMN feedback_patterns.pattern_type IS 'AVOID: patterns to filter out, PRIORITIZE: patterns to boost';
COMMENT ON COLUMN feedback_patterns.pattern_text IS 'The actual pattern (keyword, phrase, regex, etc.)';
COMMENT ON COLUMN feedback_patterns.confidence_score IS 'Confidence in pattern accuracy based on feedback success rate';
COMMENT ON COLUMN feedback_patterns.usage_count IS 'Number of times this pattern has been applied';
COMMENT ON COLUMN feedback_patterns.success_count IS 'Number of times this pattern successfully prevented bad topics or found good ones';

