-- Add collected_at column to topics table
-- This field tracks when a topic was first collected by the scanner
-- It helps distinguish recent topics from old topics

ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS collected_at TIMESTAMP;

-- For existing topics, set collected_at to created_at if it's null
UPDATE topics 
SET collected_at = created_at 
WHERE collected_at IS NULL;

-- Create index on collected_at for efficient queries
CREATE INDEX IF NOT EXISTS idx_topics_collected_at ON topics(collected_at DESC);

-- Add comment
COMMENT ON COLUMN topics.collected_at IS 'Timestamp when topic was first collected by the scanner (distinguishes recent vs old topics)';

