-- JAS-029 : Audit trail des envois sanitises au LLM
CREATE TABLE IF NOT EXISTS llm_audit_log (
    audit_id              VARCHAR(36)  PRIMARY KEY,
    session_id            VARCHAR(255) NOT NULL,
    timestamp             TIMESTAMP    NOT NULL,
    provider              VARCHAR(50)  NOT NULL,
    task_type             VARCHAR(50)  NOT NULL,
    sanitization_version  VARCHAR(20),
    payload_hash          VARCHAR(64),
    prompt_tokens_estimate INTEGER,
    degraded              BOOLEAN      NOT NULL DEFAULT false,
    degradation_reason    VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_llm_audit_session_id ON llm_audit_log (session_id);
