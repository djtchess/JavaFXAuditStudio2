-- =============================================================================
-- V4 : Tables du plan de migration (lots et risques de regression)
-- =============================================================================

CREATE TABLE migration_plan (
    id              BIGSERIAL       NOT NULL,
    session_id      VARCHAR(36)     NOT NULL,
    controller_ref  VARCHAR(512),
    compilable      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_migration_plan PRIMARY KEY (id),
    CONSTRAINT uq_migration_plan_session UNIQUE (session_id),
    CONSTRAINT fk_migration_plan_session
        FOREIGN KEY (session_id) REFERENCES analysis_session (session_id) ON DELETE CASCADE
);

CREATE TABLE planned_lot (
    id                      BIGSERIAL       NOT NULL,
    plan_id                 BIGINT          NOT NULL,
    lot_number              INTEGER         NOT NULL,
    title                   VARCHAR(256),
    objective               TEXT,
    extraction_candidates   TEXT            NOT NULL DEFAULT '[]',
    CONSTRAINT pk_planned_lot PRIMARY KEY (id),
    CONSTRAINT ck_planned_lot_number CHECK (lot_number BETWEEN 1 AND 5),
    CONSTRAINT fk_planned_lot_plan
        FOREIGN KEY (plan_id) REFERENCES migration_plan (id) ON DELETE CASCADE
);

CREATE TABLE regression_risk (
    id              BIGSERIAL       NOT NULL,
    lot_id          BIGINT          NOT NULL,
    description     TEXT,
    risk_level      VARCHAR(16),
    mitigation      TEXT,
    CONSTRAINT pk_regression_risk PRIMARY KEY (id),
    CONSTRAINT fk_regression_risk_lot
        FOREIGN KEY (lot_id) REFERENCES planned_lot (id) ON DELETE CASCADE
);

-- Index -------------------------------------------------------------------

CREATE INDEX idx_migration_plan_session     ON migration_plan (session_id);
CREATE INDEX idx_planned_lot_plan           ON planned_lot (plan_id);
CREATE INDEX idx_regression_risk_lot        ON regression_risk (lot_id);

-- Comments ----------------------------------------------------------------

COMMENT ON TABLE migration_plan IS 'Plan de migration progressive pour une session d analyse';
COMMENT ON COLUMN migration_plan.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN migration_plan.session_id IS 'UUID de la session d analyse parente';
COMMENT ON COLUMN migration_plan.controller_ref IS 'Reference au controller JavaFX analyse';
COMMENT ON COLUMN migration_plan.compilable IS 'Indique si le plan genere du code compilable';
COMMENT ON COLUMN migration_plan.created_at IS 'Horodatage UTC de creation';

COMMENT ON TABLE planned_lot IS 'Lot de migration planifie (1 a 5)';
COMMENT ON COLUMN planned_lot.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN planned_lot.plan_id IS 'Reference vers le plan de migration parent';
COMMENT ON COLUMN planned_lot.lot_number IS 'Numero du lot (1 a 5)';
COMMENT ON COLUMN planned_lot.title IS 'Titre synthetique du lot';
COMMENT ON COLUMN planned_lot.objective IS 'Objectif fonctionnel du lot';
COMMENT ON COLUMN planned_lot.extraction_candidates IS 'Liste des candidats a extraire dans ce lot';

COMMENT ON TABLE regression_risk IS 'Risque de regression associe a un lot de migration';
COMMENT ON COLUMN regression_risk.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN regression_risk.lot_id IS 'Reference vers le lot parent';
COMMENT ON COLUMN regression_risk.description IS 'Description du risque identifie';
COMMENT ON COLUMN regression_risk.risk_level IS 'Niveau de risque (LOW, MEDIUM, HIGH, CRITICAL)';
COMMENT ON COLUMN regression_risk.mitigation IS 'Strategie de mitigation proposee';
