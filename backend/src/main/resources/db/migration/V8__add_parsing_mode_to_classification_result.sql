ALTER TABLE classification_result
ADD COLUMN IF NOT EXISTS parsing_mode VARCHAR(30) NOT NULL DEFAULT 'AST';

ALTER TABLE classification_result
ADD COLUMN IF NOT EXISTS parsing_fallback_reason TEXT;
