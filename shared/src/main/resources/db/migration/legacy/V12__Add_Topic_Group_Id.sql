-- Add topic_group_id to topics table for grouping similar topics
-- Topics that cover the same or similar topics will be grouped together

-- Add topic_group_id column to topics table
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS topic_group_id BIGINT;

-- Create index on topic_group_id for efficient grouping queries
CREATE INDEX IF NOT EXISTS idx_topics_topic_group_id ON topics(topic_group_id);

-- Add comment explaining the grouping feature
COMMENT ON COLUMN topics.topic_group_id IS 'ID of the topic group this topic belongs to. Topics with the same group_id cover similar or the same topics. NULL means the topic has not been grouped yet.';

