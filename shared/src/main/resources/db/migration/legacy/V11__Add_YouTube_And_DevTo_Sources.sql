-- Add YouTube and DevTo sources to the database
-- This migration adds the new scanner sources that were implemented

-- Insert YouTube source (if it doesn't exist)
INSERT INTO sources (name, base_url, api_endpoint, is_active, scan_frequency_hours)
SELECT 'YouTube', 'https://www.youtube.com', 'https://www.googleapis.com/youtube/v3', true, 2
WHERE NOT EXISTS (
    SELECT 1 FROM sources WHERE name = 'YouTube'
);

-- Insert DevTo source (if it doesn't exist)
INSERT INTO sources (name, base_url, api_endpoint, is_active, scan_frequency_hours)
SELECT 'DevTo', 'https://dev.to', 'https://dev.to/api', true, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sources WHERE name = 'DevTo'
);

-- Insert Hashnode source (if it doesn't exist)
INSERT INTO sources (name, base_url, api_endpoint, is_active, scan_frequency_hours)
SELECT 'Hashnode', 'https://hashnode.com', 'https://api.hashnode.com', true, 2
WHERE NOT EXISTS (
    SELECT 1 FROM sources WHERE name = 'Hashnode'
);

-- Insert Medium source (if it doesn't exist)
INSERT INTO sources (name, base_url, api_endpoint, is_active, scan_frequency_hours)
SELECT 'Medium', 'https://medium.com', 'https://medium.com/feed', true, 2
WHERE NOT EXISTS (
    SELECT 1 FROM sources WHERE name = 'Medium'
);

-- Update existing sources to ensure they're active (idempotent)
UPDATE sources SET is_active = true WHERE name IN ('Reddit', 'StackOverflow');
