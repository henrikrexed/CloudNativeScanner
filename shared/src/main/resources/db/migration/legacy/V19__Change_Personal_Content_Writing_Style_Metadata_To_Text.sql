-- Change writing_style_metadata column from JSONB to TEXT
-- This fixes the type mismatch issue where Hibernate tries to insert VARCHAR into JSONB
-- TEXT is sufficient for storing JSON strings, and we can still query them as JSON if needed

ALTER TABLE personal_content 
ALTER COLUMN writing_style_metadata TYPE TEXT USING writing_style_metadata::text;

-- Update comment to reflect the change
COMMENT ON COLUMN personal_content.writing_style_metadata IS 'JSON string containing writing style analysis (tone, structure, vocabulary patterns, etc.). Stored as TEXT for compatibility with JPA/Hibernate.';
