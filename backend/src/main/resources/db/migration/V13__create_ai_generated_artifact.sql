CREATE TABLE ai_generated_artifact (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    artifact_type VARCHAR(64) NOT NULL,
    class_name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    version_number INTEGER NOT NULL,
    parent_version_id VARCHAR(36),
    request_id VARCHAR(36) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    origin_task VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_ai_generated_artifact_session_type_version
    ON ai_generated_artifact(session_id, artifact_type, version_number);

CREATE INDEX idx_ai_generated_artifact_session
    ON ai_generated_artifact(session_id);

CREATE INDEX idx_ai_generated_artifact_session_type_latest
    ON ai_generated_artifact(session_id, artifact_type, version_number DESC);
