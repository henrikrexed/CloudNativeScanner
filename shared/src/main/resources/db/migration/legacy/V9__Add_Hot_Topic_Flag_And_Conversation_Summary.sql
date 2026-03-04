-- Add hot topic flag and conversation summary support
-- Hot topics are conversations with high engagement that need full conversation collection

-- Add is_hot_topic flag to topics table
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS is_hot_topic BOOLEAN DEFAULT false;

-- Add conversation_summary column to store LLM-generated summaries
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS conversation_summary TEXT;

-- Add conversation_collected_at timestamp to track when conversation was collected
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS conversation_collected_at TIMESTAMP;

-- Add technical_quality_score for scoring technical discussions
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS technical_quality_score DECIMAL(5,2) DEFAULT 0.0;

-- Add marketing_score for detecting marketing content
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS marketing_score DECIMAL(5,2) DEFAULT 0.0;

-- Create index on is_hot_topic for efficient queries
CREATE INDEX IF NOT EXISTS idx_topics_is_hot_topic ON topics(is_hot_topic) WHERE is_hot_topic = true;

-- Create index on technical_quality_score for filtering high-quality technical content
CREATE INDEX IF NOT EXISTS idx_topics_technical_quality_score ON topics(technical_quality_score DESC);

-- Add comments
COMMENT ON COLUMN topics.is_hot_topic IS 'Flag indicating if this topic has high engagement and needs full conversation collection';
COMMENT ON COLUMN topics.conversation_summary IS 'LLM-generated summary of the full conversation thread';
COMMENT ON COLUMN topics.conversation_collected_at IS 'Timestamp when the full conversation was collected from the source';
COMMENT ON COLUMN topics.technical_quality_score IS 'Score from 0.0 to 1.0 indicating technical quality (higher = more technical)';
COMMENT ON COLUMN topics.marketing_score IS 'Score from 0.0 to 1.0 indicating marketing content (higher = more marketing)';
