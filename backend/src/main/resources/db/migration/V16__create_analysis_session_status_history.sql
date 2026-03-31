CREATE TABLE analysis_session_status_history (
    id          BIGSERIAL PRIMARY KEY,
    session_id  VARCHAR(36) NOT NULL,
    status      VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_analysis_session_status_history_session
        FOREIGN KEY (session_id) REFERENCES analysis_session (session_id) ON DELETE CASCADE
);

CREATE INDEX idx_analysis_session_status_history_session
    ON analysis_session_status_history (session_id, occurred_at);

COMMENT ON TABLE analysis_session_status_history IS 'Historique des transitions de statut d une session d analyse';
COMMENT ON COLUMN analysis_session_status_history.session_id IS 'UUID de la session d analyse';
COMMENT ON COLUMN analysis_session_status_history.status IS 'Statut persiste a l instant de la transition';
COMMENT ON COLUMN analysis_session_status_history.occurred_at IS 'Horodatage UTC de la transition';
