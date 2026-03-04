-- Add content extraction status flag to topics table
-- This flag indicates whether content has been extracted and processed

ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS content_extraction_status VARCHAR(50) DEFAULT 'PENDING';

ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS content_extraction_attempted_at TIMESTAMP;

ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS content_extraction_completed_at TIMESTAMP;

ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS content_summary TEXT;

-- Create index for efficient querying of topics needing content extraction
CREATE INDEX IF NOT EXISTS idx_topics_content_extraction_status 
ON topics(content_extraction_status) 
WHERE content_extraction_status = 'PENDING';

-- Update existing topics to have PENDING status if they don't have content
UPDATE topics 
SET content_extraction_status = 'PENDING' 
WHERE content_extraction_status IS NULL 
  AND (content IS NULL OR content = '' OR LENGTH(content) < 500);

-- Update existing topics to have COMPLETED status if they have substantial content
UPDATE topics 
SET content_extraction_status = 'COMPLETED',
    content_extraction_completed_at = updated_at
WHERE content_extraction_status IS NULL 
  AND content IS NOT NULL 
  AND content != '' 
  AND LENGTH(content) >= 500;

COMMENT ON COLUMN topics.content_extraction_status IS 'Status of content extraction: PENDING, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN topics.content_extraction_attempted_at IS 'Timestamp when content extraction was last attempted';
COMMENT ON COLUMN topics.content_extraction_completed_at IS 'Timestamp when content extraction was completed';
COMMENT ON COLUMN topics.content_summary IS 'LLM-generated summary of the extracted content for RAG storage';
