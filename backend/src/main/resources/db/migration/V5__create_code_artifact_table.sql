-- =============================================================================
-- V5 : Table des artefacts de code generes
-- =============================================================================

CREATE TABLE code_artifact (
    id                      BIGSERIAL       NOT NULL,
    session_id              VARCHAR(36)     NOT NULL,
    controller_ref          VARCHAR(512),
    artifact_id             VARCHAR(128)    NOT NULL,
    artifact_type           VARCHAR(32),
    lot_number              INTEGER,
    class_name              VARCHAR(256),
    content                 TEXT,
    transitional_bridge     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_code_artifact PRIMARY KEY (id),
    CONSTRAINT uq_code_artifact_artifact_id UNIQUE (artifact_id),
    CONSTRAINT fk_code_artifact_session
        FOREIGN KEY (session_id) REFERENCES analysis_session (session_id) ON DELETE CASCADE
);

-- Index -------------------------------------------------------------------

CREATE INDEX idx_code_artifact_session      ON code_artifact (session_id);
CREATE INDEX idx_code_artifact_lot_number   ON code_artifact (lot_number);

-- Comments ----------------------------------------------------------------

COMMENT ON TABLE code_artifact IS 'Artefact de code genere lors de la migration';
COMMENT ON COLUMN code_artifact.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN code_artifact.session_id IS 'UUID de la session d analyse parente';
COMMENT ON COLUMN code_artifact.controller_ref IS 'Reference au controller JavaFX source';
COMMENT ON COLUMN code_artifact.artifact_id IS 'Identifiant fonctionnel unique de l artefact';
COMMENT ON COLUMN code_artifact.artifact_type IS 'Type d artefact (SERVICE, VALIDATOR, CONVERTER, CONTROLLER, etc.)';
COMMENT ON COLUMN code_artifact.lot_number IS 'Numero du lot de migration associe';
COMMENT ON COLUMN code_artifact.class_name IS 'Nom de la classe Java generee';
COMMENT ON COLUMN code_artifact.content IS 'Code source genere';
COMMENT ON COLUMN code_artifact.transitional_bridge IS 'Indique si l artefact est un pont de transition';
COMMENT ON COLUMN code_artifact.created_at IS 'Horodatage UTC de creation';
