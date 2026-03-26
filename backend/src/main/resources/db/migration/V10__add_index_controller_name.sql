-- V10 : Index performance sur analysis_session.controller_name
-- Utilise par les requetes d'agregation du dashboard projet (JAS-015)
-- Optimise COUNT(*) WHERE controller_name = :projectId

CREATE INDEX IF NOT EXISTS idx_analysis_session_controller_name
    ON analysis_session (controller_name);
