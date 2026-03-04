-- Add feedback fields to topics table
-- These fields track user feedback (thumbs up/down) to improve scanner quality

-- Add feedback columns
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS thumbs_up INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS thumbs_down INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS is_rejected BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
ADD COLUMN IF NOT EXISTS feedback_learned BOOLEAN DEFAULT false;

-- Create index on is_rejected for efficient filtering
CREATE INDEX IF NOT EXISTS idx_topics_is_rejected ON topics(is_rejected);

-- Create index on feedback_learned to track which topics have been processed
CREATE INDEX IF NOT EXISTS idx_topics_feedback_learned ON topics(feedback_learned);

-- Add comments explaining the feedback feature
COMMENT ON COLUMN topics.thumbs_up IS 'Number of thumbs up votes. Positive feedback indicates valuable content.';
COMMENT ON COLUMN topics.thumbs_down IS 'Number of thumbs down votes. Negative feedback indicates low-quality or irrelevant content.';
COMMENT ON COLUMN topics.is_rejected IS 'Whether this topic has been rejected (thumbs down). Rejected topics are hidden and help improve scanner filtering.';
COMMENT ON COLUMN topics.rejection_reason IS 'Reason for rejection (e.g., "not relevant", "marketing content", "low quality").';
COMMENT ON COLUMN topics.feedback_learned IS 'Whether feedback from this topic has been used to improve scanner filtering logic.';

