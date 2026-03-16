package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.cartography.CartographyUnknown;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;

import java.util.List;

public interface CartographyAnalysisPort {

    List<FxmlComponent> extractComponents(String fxmlContent);

    List<HandlerBinding> extractHandlers(String javaContent);

    /**
     * Extrait les elements non standard du FXML : imports, fx:include, bindings, stylesheets.
     * Retourne une liste vide par defaut pour compatibilite arriere.
     *
     * @param fxmlContent contenu FXML brut
     * @return liste de CartographyUnknown, jamais null
     */
    default List<CartographyUnknown> extractUnknowns(String fxmlContent) {
        return List.of();
    }
}
