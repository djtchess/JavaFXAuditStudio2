COMMENT ON COLUMN analysis_session.status IS 'Statut courant : CREATED, PENDING, IN_PROGRESS, RUNNING, INGESTING, CARTOGRAPHING, CLASSIFYING, PLANNING, GENERATING, REPORTING, COMPLETED, FAILED, LOCKED';

ALTER TABLE analysis_session
    DROP CONSTRAINT IF EXISTS ck_analysis_session_status;

ALTER TABLE analysis_session
    ADD CONSTRAINT ck_analysis_session_status
    CHECK (status IN (
        'CREATED',
        'PENDING',
        'IN_PROGRESS',
        'RUNNING',
        'INGESTING',
        'CARTOGRAPHING',
        'CLASSIFYING',
        'PLANNING',
        'GENERATING',
        'REPORTING',
        'COMPLETED',
        'FAILED',
        'LOCKED'
    ));
