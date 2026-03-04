-- Reset existing topics to PENDING status for content extraction
-- This allows the ContentExtractionProcessor to pick up existing topics that haven't been processed yet
-- Only reset topics that:
--   1. Don't have COMPLETED status, OR
--   2. Have COMPLETED status but content is missing or too short (< 500 chars), OR
--   3. Have NULL status (shouldn't happen, but just in case)

-- Reset topics that are not COMPLETED or have insufficient content
UPDATE topics 
SET content_extraction_status = 'PENDING',
    content_extraction_attempted_at = NULL,
    content_extraction_completed_at = NULL
WHERE (content_extraction_status IS NULL 
   OR content_extraction_status != 'COMPLETED'
   OR (content_extraction_status = 'COMPLETED' AND (content IS NULL OR content = '' OR LENGTH(content) < 500)))
  AND url IS NOT NULL 
  AND url != ''
  AND (is_rejected IS NULL OR is_rejected = false);

-- Log how many topics were reset (this will be visible in migration logs)
DO $$
DECLARE
    reset_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO reset_count
    FROM topics
    WHERE content_extraction_status = 'PENDING'
      AND url IS NOT NULL 
      AND url != ''
      AND (is_rejected IS NULL OR is_rejected = false);
    
    RAISE NOTICE 'Reset % topics to PENDING status for content extraction', reset_count;
END $$;

COMMENT ON COLUMN topics.content_extraction_status IS 'Status of content extraction: PENDING (needs processing), PROCESSING (currently being processed), COMPLETED (successfully processed), FAILED (processing failed). Reset to PENDING to reprocess existing topics.';
