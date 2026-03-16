-- =============================================================================
-- V2 : Tables de cartographie FXML (composants, bindings, inconnues)
-- =============================================================================

CREATE TABLE cartography_result (
    id              BIGSERIAL       NOT NULL,
    session_id      VARCHAR(36)     NOT NULL,
    controller_ref  VARCHAR(512),
    fxml_ref        VARCHAR(512),
    has_unknowns    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_cartography_result PRIMARY KEY (id),
    CONSTRAINT uq_cartography_result_session UNIQUE (session_id),
    CONSTRAINT fk_cartography_result_session
        FOREIGN KEY (session_id) REFERENCES analysis_session (session_id) ON DELETE CASCADE
);

CREATE TABLE fxml_component (
    id              BIGSERIAL       NOT NULL,
    cartography_id  BIGINT          NOT NULL,
    fx_id           VARCHAR(256),
    component_type  VARCHAR(256),
    event_handler   VARCHAR(256)    NOT NULL DEFAULT '',
    CONSTRAINT pk_fxml_component PRIMARY KEY (id),
    CONSTRAINT fk_fxml_component_cartography
        FOREIGN KEY (cartography_id) REFERENCES cartography_result (id) ON DELETE CASCADE
);

CREATE TABLE handler_binding (
    id              BIGSERIAL       NOT NULL,
    cartography_id  BIGINT          NOT NULL,
    method_name     VARCHAR(256),
    fxml_ref        VARCHAR(512),
    injected_type   VARCHAR(256),
    CONSTRAINT pk_handler_binding PRIMARY KEY (id),
    CONSTRAINT fk_handler_binding_cartography
        FOREIGN KEY (cartography_id) REFERENCES cartography_result (id) ON DELETE CASCADE
);

CREATE TABLE cartography_unknown (
    id              BIGSERIAL       NOT NULL,
    cartography_id  BIGINT          NOT NULL,
    location        VARCHAR(512),
    reason          VARCHAR(1024),
    CONSTRAINT pk_cartography_unknown PRIMARY KEY (id),
    CONSTRAINT fk_cartography_unknown_cartography
        FOREIGN KEY (cartography_id) REFERENCES cartography_result (id) ON DELETE CASCADE
);

-- Index -------------------------------------------------------------------

CREATE INDEX idx_cartography_result_session    ON cartography_result (session_id);
CREATE INDEX idx_fxml_component_cartography    ON fxml_component (cartography_id);
CREATE INDEX idx_handler_binding_cartography   ON handler_binding (cartography_id);
CREATE INDEX idx_cartography_unknown_cartography ON cartography_unknown (cartography_id);

-- Comments ----------------------------------------------------------------

COMMENT ON TABLE cartography_result IS 'Resultat de cartographie FXML pour une session d analyse';
COMMENT ON COLUMN cartography_result.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN cartography_result.session_id IS 'UUID de la session d analyse parente';
COMMENT ON COLUMN cartography_result.controller_ref IS 'Reference au controller JavaFX analyse';
COMMENT ON COLUMN cartography_result.fxml_ref IS 'Reference au fichier FXML associe';
COMMENT ON COLUMN cartography_result.has_unknowns IS 'Indique la presence d elements non resolus';
COMMENT ON COLUMN cartography_result.created_at IS 'Horodatage UTC de creation';

COMMENT ON TABLE fxml_component IS 'Composant FXML identifie dans la cartographie';
COMMENT ON COLUMN fxml_component.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN fxml_component.cartography_id IS 'Reference vers la cartographie parente';
COMMENT ON COLUMN fxml_component.fx_id IS 'Identifiant fx:id du composant FXML';
COMMENT ON COLUMN fxml_component.component_type IS 'Type du composant (Button, TextField, etc.)';
COMMENT ON COLUMN fxml_component.event_handler IS 'Nom du handler d evenement associe';

COMMENT ON TABLE handler_binding IS 'Liaison handler/methode detectee dans la cartographie';
COMMENT ON COLUMN handler_binding.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN handler_binding.cartography_id IS 'Reference vers la cartographie parente';
COMMENT ON COLUMN handler_binding.method_name IS 'Nom de la methode Java du handler';
COMMENT ON COLUMN handler_binding.fxml_ref IS 'Reference FXML source du binding';
COMMENT ON COLUMN handler_binding.injected_type IS 'Type injecte utilise par le handler';

COMMENT ON TABLE cartography_unknown IS 'Element non resolu detecte lors de la cartographie';
COMMENT ON COLUMN cartography_unknown.id IS 'Identifiant technique auto-incremente';
COMMENT ON COLUMN cartography_unknown.cartography_id IS 'Reference vers la cartographie parente';
COMMENT ON COLUMN cartography_unknown.location IS 'Emplacement de l element non resolu';
COMMENT ON COLUMN cartography_unknown.reason IS 'Raison pour laquelle l element n a pas ete resolu';
