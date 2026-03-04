-- Add enabled column to themes table
-- This allows users to disable themes, which will stop collecting topics for that theme
ALTER TABLE themes ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true;

-- Create index for better query performance when filtering enabled themes
CREATE INDEX IF NOT EXISTS idx_themes_enabled ON themes(enabled);

-- Update any existing NULL values to true (shouldn't happen with NOT NULL, but just in case)
UPDATE themes SET enabled = true WHERE enabled IS NULL;


