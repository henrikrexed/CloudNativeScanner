-- Add parent theme support for hierarchical sub-themes (e.g., "Kubernetes/Networking")
-- This allows themes to have parent-child relationships for better categorization

-- Add parent_theme_id to themes table for hierarchical organization
ALTER TABLE themes 
ADD COLUMN IF NOT EXISTS parent_theme_id BIGINT REFERENCES themes(id) ON DELETE SET NULL;

-- Add engagement_score to topics table
-- This score is calculated based on interactions, replies, views, and other engagement metrics
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS engagement_score DECIMAL(5,2) DEFAULT 0.0;

-- Create index on parent_theme_id for efficient hierarchical queries
CREATE INDEX IF NOT EXISTS idx_themes_parent_theme_id ON themes(parent_theme_id);

-- Create index on engagement_score for sorting/filtering by engagement
CREATE INDEX IF NOT EXISTS idx_topics_engagement_score ON topics(engagement_score);

-- Add comment explaining the engagement score calculation
COMMENT ON COLUMN topics.engagement_score IS 'Calculated score based on interactions, replies, views, and other engagement metrics. Higher scores indicate more engaging topics.';

-- Add comment explaining the parent theme relationship
COMMENT ON COLUMN themes.parent_theme_id IS 'Reference to parent theme for hierarchical organization (e.g., "Networking" under "Kubernetes"). NULL indicates a top-level theme.';
