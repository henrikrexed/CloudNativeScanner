-- Add missing columns to topics table to match Topic entity
-- These columns were added to the Java entity but not to the database schema

-- Add author column (nullable, as some topics may not have an author)
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS author VARCHAR(255);

-- Add view_count column (default 0)
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS view_count INTEGER DEFAULT 0;

-- Add score column (default 0)
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS score INTEGER DEFAULT 0;

-- Add last_scanned_at column (timestamp)
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS last_scanned_at TIMESTAMP;

-- Update existing rows to set default values if columns were just added
UPDATE topics 
SET view_count = 0 
WHERE view_count IS NULL;

UPDATE topics 
SET score = 0 
WHERE score IS NULL;

UPDATE topics 
SET last_scanned_at = updated_at 
WHERE last_scanned_at IS NULL;

-- Create index on last_scanned_at for better query performance
CREATE INDEX IF NOT EXISTS idx_topics_last_scanned_at ON topics(last_scanned_at);

-- Add engagement_score column for sorting hottest conversations
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS engagement_score DECIMAL(5,2) DEFAULT 0.00;

-- Create index on engagement_score for better query performance
CREATE INDEX IF NOT EXISTS idx_topics_engagement_score ON topics(engagement_score DESC NULLS LAST);
