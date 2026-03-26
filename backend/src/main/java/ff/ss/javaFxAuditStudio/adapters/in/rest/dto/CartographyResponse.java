package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Cartographie FXML : composants et bindings handlers")
public record CartographyResponse(
        @Schema(description = "Reference du controller JavaFX")
        String controllerRef,
        @Schema(description = "Reference du fichier FXML associe")
        String fxmlRef,
        @Schema(description = "Liste des composants FXML (fx:id, type)")
        List<FxmlComponentDto> components,
        @Schema(description = "Liste des bindings handler/evenement")
        List<HandlerBindingDto> handlers,
        @Schema(description = "Indique la presence de types non resolus")
        boolean hasUnknowns) {

    public CartographyResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(fxmlRef, "fxmlRef must not be null");
        Objects.requireNonNull(components, "components must not be null");
        Objects.requireNonNull(handlers, "handlers must not be null");
        components = List.copyOf(components);
        handlers = List.copyOf(handlers);
    }

    @Schema(description = "Composant FXML identifie par fx:id")
    public record FxmlComponentDto(
            @Schema(description = "Identifiant fx:id du composant dans le fichier FXML")
            String fxId,
            @Schema(description = "Type Java du composant (ex: Button, TableView)")
            String componentType,
            @Schema(description = "Handler d'evenement lie a ce composant, vide si aucun")
            String eventHandler) {

        public FxmlComponentDto {
            Objects.requireNonNull(fxId, "fxId must not be null");
            Objects.requireNonNull(componentType, "componentType must not be null");
            Objects.requireNonNull(eventHandler, "eventHandler must not be null");
        }
    }

    @Schema(description = "Binding entre un handler @FXML et son evenement")
    public record HandlerBindingDto(
            @Schema(description = "Nom de la methode handler annotee @FXML")
            String methodName,
            @Schema(description = "Reference FXML du composant source de l'evenement")
            String fxmlRef,
            @Schema(description = "Type injecte par @FXML dans le handler")
            String injectedType) {

        public HandlerBindingDto {
            Objects.requireNonNull(methodName, "methodName must not be null");
            Objects.requireNonNull(fxmlRef, "fxmlRef must not be null");
            Objects.requireNonNull(injectedType, "injectedType must not be null");
        }
    }
}
