-- =============================================================================
-- V6 : Table du rapport de restitution consolide
-- =============================================================================

CREATE TABLE restitution_report (
    id                  BIGSERIAL       NOT NULL,
    session_id          VARCHAR(36)     NOT NULL,
    controller_ref      VARCHAR(512),
    rule_count          INTEGER         NOT NULL DEFAULT 0,
    uncertain_count     INTEGER         NOT NULL DEFAULT 0,
    artifact_count      INTEGER         NOT NULL DEFAULT 0,
    bridge_count        INTEGER         NOT NULL DEFAULT 0,
    confidence          VARCHAR(16),
    has_contradictions  BOOLEAN         NOT NULL DEFAULT FALSE,
    contradictions      TEXT            NOT NULL DEFAULT '[]',
    unknowns            TEXT            NOT NULL DEFAULT '[]',
    findings            TEXT            NOT NULL DEFAULT '[]',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_restitution_report PRIMARY KEY (id),
    CONSTRAINT uq_restitution_report_session UNIQUE (session_id),
    CONSTRAINT fk_restitution_report_session
        FOREIGN KEY (session_id) REFERENCES analysis_session (session_id) ON DELETE CASCADE
);

-- Index -------------------------------------------------------------------

CREATE INDEX idx_restitution_report_session ON restitution_report (session_id);

-- Comments ----------------------------------------------------------------

COMMENT ON TABLE restitution_report IS 'Rapport de restitution consolide pour une session d analyse';
COMMENT ON COLUMN restitution_report.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN restitution_report.session_id IS 'UUID de la session d analyse parente';
COMMENT ON COLUMN restitution_report.controller_ref IS 'Reference au controller JavaFX analyse';
COMMENT ON COLUMN restitution_report.rule_count IS 'Nombre total de regles metier detectees';
COMMENT ON COLUMN restitution_report.uncertain_count IS 'Nombre de regles classifiees comme incertaines';
COMMENT ON COLUMN restitution_report.artifact_count IS 'Nombre d artefacts de code generes';
COMMENT ON COLUMN restitution_report.bridge_count IS 'Nombre de ponts de transition generes';
COMMENT ON COLUMN restitution_report.confidence IS 'Niveau de confiance global (LOW, MEDIUM, HIGH)';
COMMENT ON COLUMN restitution_report.has_contradictions IS 'Indique la presence de contradictions detectees';
COMMENT ON COLUMN restitution_report.contradictions IS 'Liste des contradictions au format JSON';
COMMENT ON COLUMN restitution_report.unknowns IS 'Liste des inconnues non resolues au format JSON';
COMMENT ON COLUMN restitution_report.findings IS 'Observations et recommandations au format JSON';
COMMENT ON COLUMN restitution_report.created_at IS 'Horodatage UTC de creation';
