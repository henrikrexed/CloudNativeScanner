-- Create personal_content table for storing user's personal content
CREATE TABLE personal_content (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL', -- GENERAL, BLOG, VIDEO_SCRIPT, ARTICLE, etc.
    category VARCHAR(100), -- Main category for broader organization (e.g., "Tutorial", "Documentation", "Case Study")
    tags TEXT, -- JSON array of tags for detailed indexing (e.g., ["kubernetes", "beginner", "tutorial", "networking"])
    user_id VARCHAR(255), -- For future multi-user support, can be null for now
    writing_style_metadata TEXT, -- Store writing style analysis (tone, structure, vocabulary, etc.) as JSON string
    rag_stored BOOLEAN DEFAULT false, -- Flag to track if content is stored in RAG
    rag_stored_at TIMESTAMP, -- When content was stored in RAG
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_personal_content_user_id ON personal_content(user_id);
CREATE INDEX idx_personal_content_content_type ON personal_content(content_type);
CREATE INDEX idx_personal_content_category ON personal_content(category);
CREATE INDEX idx_personal_content_rag_stored ON personal_content(rag_stored);
CREATE INDEX idx_personal_content_created_at ON personal_content(created_at);
-- Create GIN index for tag search (full-text search on tags)
CREATE INDEX IF NOT EXISTS idx_personal_content_tags ON personal_content USING gin(to_tsvector('english', tags));

-- Add comment for documentation
COMMENT ON TABLE personal_content IS 'Stores personal content that users paste to feed into RAG system';
COMMENT ON COLUMN personal_content.category IS 'Main category for broader organization (e.g., "Tutorial", "Documentation", "Case Study", "Blog Post")';
COMMENT ON COLUMN personal_content.tags IS 'JSON array of tags for detailed indexing and categorization (e.g., ["kubernetes", "beginner", "tutorial", "networking"]). Used for filtering and searching content.';
COMMENT ON COLUMN personal_content.writing_style_metadata IS 'JSON object containing writing style analysis (tone, structure, vocabulary patterns, etc.)';
COMMENT ON COLUMN personal_content.rag_stored IS 'Indicates if this content has been processed and stored in the RAG vector database';
