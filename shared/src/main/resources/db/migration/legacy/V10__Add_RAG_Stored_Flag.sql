-- Add flag to track when conversation summary has been stored in RAG
-- This ensures we don't re-process topics that are already in RAG

-- Add conversation_rag_stored flag to topics table
ALTER TABLE topics 
ADD COLUMN IF NOT EXISTS conversation_rag_stored BOOLEAN DEFAULT false;

-- Create index on conversation_rag_stored for efficient queries
CREATE INDEX IF NOT EXISTS idx_topics_conversation_rag_stored ON topics(conversation_rag_stored) WHERE conversation_rag_stored = true;

-- Add comment
COMMENT ON COLUMN topics.conversation_rag_stored IS 'Flag indicating if the conversation summary has been successfully stored in RAG vector database';
