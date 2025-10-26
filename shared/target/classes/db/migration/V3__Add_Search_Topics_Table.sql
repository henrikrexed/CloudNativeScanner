-- Add search topics table for configurable topic searches
CREATE TABLE search_topics (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    keyword VARCHAR(255) NOT NULL,
    search_query VARCHAR(500),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 1, -- 1=high, 2=medium, 3=low
    max_results INTEGER DEFAULT 50,
    search_frequency_hours INTEGER DEFAULT 24,
    last_searched_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_search_topics_source_id ON search_topics(source_id);
CREATE INDEX idx_search_topics_keyword ON search_topics(keyword);
CREATE INDEX idx_search_topics_is_active ON search_topics(is_active);
CREATE INDEX idx_search_topics_priority ON search_topics(priority);
CREATE INDEX idx_search_topics_last_searched_at ON search_topics(last_searched_at);

-- Insert default search topics for StackOverflow
INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'kubernetes', 'kubernetes', 'Kubernetes-related questions and discussions', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'docker', 'docker', 'Docker containerization questions', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'microservices', 'microservices', 'Microservices architecture discussions', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'cloud-native', 'cloud native', 'Cloud-native development topics', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'devops', 'devops', 'DevOps practices and tools', 2, 30
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'containers', 'containers', 'Container technology discussions', 2, 30
FROM sources s WHERE s.name = 'StackOverflow';

-- Insert default search topics for Reddit
INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'kubernetes', 'kubernetes', 'Kubernetes community discussions', 1, 25
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'docker', 'docker', 'Docker community discussions', 1, 25
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'devops', 'devops', 'DevOps community discussions', 1, 25
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'cloudnative', 'cloudnative', 'Cloud Native community discussions', 1, 25
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'microservices', 'microservices', 'Microservices community discussions', 2, 20
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'openshift', 'openshift', 'OpenShift community discussions', 2, 20
FROM sources s WHERE s.name = 'Reddit';


