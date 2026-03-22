-- JAS-013 : Table d'audit pour les reclassifications manuelles de regles de gestion.
-- Chaque ligne represente une reclassification effectuee par un utilisateur.

CREATE TABLE IF NOT EXISTS rule_classification_audit (
    id            VARCHAR(36)  PRIMARY KEY,
    analysis_id   VARCHAR(255) NOT NULL,
    rule_id       VARCHAR(255) NOT NULL,
    from_category VARCHAR(50)  NOT NULL,
    to_category   VARCHAR(50)  NOT NULL,
    reason        TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_analysis_rule
    ON rule_classification_audit(analysis_id, rule_id);
