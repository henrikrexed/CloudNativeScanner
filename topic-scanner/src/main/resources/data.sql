-- Insert default themes
INSERT INTO themes (name, description) VALUES 
('Cloud Native', 'Topics related to cloud-native technologies, containers, microservices'),
('Kubernetes', 'Kubernetes-specific discussions, deployments, and configurations'),
('DevOps', 'DevOps practices, CI/CD, automation, and infrastructure'),
('Security', 'Security-related topics, vulnerabilities, and best practices'),
('Monitoring', 'Observability, monitoring, logging, and alerting'),
('Development', 'General development topics, programming languages, frameworks'),
('Architecture', 'System design, architecture patterns, and scalability'),
('Performance', 'Performance optimization, tuning, and benchmarking');

-- Insert default sources
INSERT INTO sources (name, base_url, api_endpoint, scan_frequency_hours) VALUES 
('StackOverflow', 'https://stackoverflow.com', 'https://api.stackexchange.com/2.3', 24),
('Reddit', 'https://reddit.com', 'https://www.reddit.com/r', 24);

-- Insert search topics for StackOverflow
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
SELECT s.id, 'spring-boot', 'spring boot', 'Spring Boot framework questions', 2, 30
FROM sources s WHERE s.name = 'StackOverflow';

-- Insert search topics for Reddit
INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'kubernetes', 'kubernetes', 'Kubernetes discussions on Reddit', 1, 50
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'docker', 'docker', 'Docker discussions on Reddit', 1, 50
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'devops', 'devops', 'DevOps discussions on Reddit', 1, 50
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'cloud-native', 'cloud native', 'Cloud-native discussions on Reddit', 1, 50
FROM sources s WHERE s.name = 'Reddit';

