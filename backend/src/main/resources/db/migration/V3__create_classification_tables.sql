-- =============================================================================
-- V3 : Tables de classification des regles metier
-- =============================================================================

CREATE TABLE classification_result (
    id              BIGSERIAL       NOT NULL,
    session_id      VARCHAR(36)     NOT NULL,
    controller_ref  VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_classification_result PRIMARY KEY (id),
    CONSTRAINT uq_classification_result_session UNIQUE (session_id),
    CONSTRAINT fk_classification_result_session
        FOREIGN KEY (session_id) REFERENCES analysis_session (session_id) ON DELETE CASCADE
);

CREATE TABLE business_rule (
    id                      BIGSERIAL       NOT NULL,
    classification_id       BIGINT          NOT NULL,
    rule_id                 VARCHAR(64),
    description             TEXT,
    source_ref              VARCHAR(512),
    source_line             INTEGER         NOT NULL DEFAULT 0,
    responsibility_class    VARCHAR(32),
    extraction_candidate    VARCHAR(32),
    uncertain               BOOLEAN         NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_business_rule PRIMARY KEY (id),
    CONSTRAINT fk_business_rule_classification
        FOREIGN KEY (classification_id) REFERENCES classification_result (id) ON DELETE CASCADE
);

-- Index -------------------------------------------------------------------

CREATE INDEX idx_classification_result_session ON classification_result (session_id);
CREATE INDEX idx_business_rule_classification  ON business_rule (classification_id);

-- Comments ----------------------------------------------------------------

COMMENT ON TABLE classification_result IS 'Resultat de classification des regles metier pour une session';
COMMENT ON COLUMN classification_result.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN classification_result.session_id IS 'UUID de la session d analyse parente';
COMMENT ON COLUMN classification_result.controller_ref IS 'Reference au controller JavaFX analyse';
COMMENT ON COLUMN classification_result.created_at IS 'Horodatage UTC de creation';

COMMENT ON TABLE business_rule IS 'Regle metier extraite et classifiee';
COMMENT ON COLUMN business_rule.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN business_rule.classification_id IS 'Reference vers le resultat de classification parent';
COMMENT ON COLUMN business_rule.rule_id IS 'Identifiant fonctionnel de la regle (ex. BR-001)';
COMMENT ON COLUMN business_rule.description IS 'Description en langage naturel de la regle';
COMMENT ON COLUMN business_rule.source_ref IS 'Fichier source ou la regle a ete detectee';
COMMENT ON COLUMN business_rule.source_line IS 'Numero de ligne dans le fichier source';
COMMENT ON COLUMN business_rule.responsibility_class IS 'Classe de responsabilite (UI, BUSINESS, TECHNICAL, MIXED)';
COMMENT ON COLUMN business_rule.extraction_candidate IS 'Candidat d extraction (SERVICE, VALIDATOR, CONVERTER, etc.)';
COMMENT ON COLUMN business_rule.uncertain IS 'Indique si la classification est incertaine';
