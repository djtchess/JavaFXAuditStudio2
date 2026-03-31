ALTER TABLE restitution_report
    ADD COLUMN markdown TEXT NOT NULL DEFAULT '';

ALTER TABLE restitution_report
    ADD COLUMN lot_summaries TEXT NOT NULL DEFAULT '[]';

ALTER TABLE restitution_report
    ADD COLUMN artifact_summaries TEXT NOT NULL DEFAULT '[]';

COMMENT ON COLUMN restitution_report.markdown IS 'Version markdown complete de la restitution';
COMMENT ON COLUMN restitution_report.lot_summaries IS 'Liste des lots de migration au format JSON';
COMMENT ON COLUMN restitution_report.artifact_summaries IS 'Liste des artefacts generes au format JSON';
