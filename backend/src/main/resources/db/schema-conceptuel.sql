-- Schema conceptuel pour JavaFXAuditStudio2
-- Story : JAS-12 - Persistance PostgreSQL des sessions d'analyse
-- Ce fichier est un schema de reference conceptuel.
-- Les migrations executables seront gerees par Flyway (hors perimetre JAS-12).

CREATE TABLE analysis_session (

    -- Identifiant metier de la session, genere par l'application (UUID ou autre).
    session_id VARCHAR NOT NULL,

    -- Nom qualifie ou simple du controller JavaFX analyse dans cette session.
    controller_name VARCHAR NOT NULL,

    -- Reference externe vers l'extrait de source analyse (chemin, URI ou identifiant
    -- de stockage object). Le contenu brut n'est pas stocke en base pour eviter
    -- la duplication et limiter la taille des lignes.
    source_snippet_ref VARCHAR,

    -- Etat courant du cycle de vie de la session.
    -- Valeurs attendues : PENDING, RUNNING, COMPLETED, FAILED.
    status VARCHAR NOT NULL,

    -- Horodatage de creation de la session, toujours en UTC.
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_analysis_session PRIMARY KEY (session_id)
);
