-- Fix confidence_score column type to match Hibernate entity definition
-- PostgreSQL NUMERIC and DECIMAL are the same, but Hibernate validation is strict
-- This migration ensures the column type exactly matches DECIMAL(3,2)
ALTER TABLE topic_themes 
  ALTER COLUMN confidence_score TYPE DECIMAL(3,2) 
  USING confidence_score::DECIMAL(3,2);

-- Ensure created_at column exists (in case it was missing)
ALTER TABLE topic_themes 
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;











