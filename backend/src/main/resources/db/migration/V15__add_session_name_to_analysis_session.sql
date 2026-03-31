ALTER TABLE analysis_session
    ADD COLUMN session_name VARCHAR(255);

UPDATE analysis_session
SET session_name = controller_name
WHERE session_name IS NULL;

ALTER TABLE analysis_session
    ALTER COLUMN session_name SET NOT NULL;

COMMENT ON COLUMN analysis_session.session_name IS 'Nom fonctionnel de la session d analyse';
COMMENT ON COLUMN analysis_session.status IS 'Statut courant : CREATED, PENDING, IN_PROGRESS, RUNNING, INGESTING, CARTOGRAPHING, CLASSIFYING, PLANNING, GENERATING, REPORTING, COMPLETED, FAILED, LOCKED';
