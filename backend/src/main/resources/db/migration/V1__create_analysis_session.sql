CREATE TABLE analysis_session (
    session_id      VARCHAR(36)  NOT NULL,
    controller_name VARCHAR(512) NOT NULL,
    source_snippet_ref VARCHAR(1024),
    status          VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_analysis_session PRIMARY KEY (session_id)
);
COMMENT ON TABLE analysis_session IS 'Sessions d analyse de controllers JavaFX';
COMMENT ON COLUMN analysis_session.session_id IS 'UUID de la session';
COMMENT ON COLUMN analysis_session.controller_name IS 'Nom du controller analyse';
COMMENT ON COLUMN analysis_session.source_snippet_ref IS 'Reference optionnelle au snippet source';
COMMENT ON COLUMN analysis_session.status IS 'Statut : PENDING, RUNNING, COMPLETED, FAILED';
COMMENT ON COLUMN analysis_session.created_at IS 'Horodatage UTC de creation';
