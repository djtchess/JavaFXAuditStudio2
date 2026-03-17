-- =============================================================================
-- V7 : Correction contrainte unique sur code_artifact
-- La contrainte portait uniquement sur artifact_id, ce qui interdisait de
-- regenerer les artefacts pour un meme controller dans une nouvelle session.
-- On la remplace par une contrainte composite (session_id, artifact_id).
-- =============================================================================

ALTER TABLE code_artifact
    DROP CONSTRAINT uq_code_artifact_artifact_id;

ALTER TABLE code_artifact
    ADD CONSTRAINT uq_code_artifact_session_artifact UNIQUE (session_id, artifact_id);
