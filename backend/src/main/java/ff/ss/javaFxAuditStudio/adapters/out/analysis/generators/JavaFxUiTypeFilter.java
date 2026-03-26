package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.rules.MethodParameter;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;

import java.util.List;
import java.util.Set;

/**
 * Filtre les types JavaFX propres a la couche UI (evenements, composants)
 * des signatures destinees a la couche domaine / application.
 *
 * <p>Les types d'evenements JavaFX (@FXML obligatoire) ne doivent pas
 * fuiter dans les interfaces UseCase qui sont des ports d'entree domaine purs.
 * Le SlimController conserve ces parametres pour satisfaire l'API @FXML.
 */
public final class JavaFxUiTypeFilter {

    // Types d'evenements JavaFX a exclure des ports d'entree domaine
    private static final Set<String> JAVAFX_EVENT_TYPES = Set.of(
            "ActionEvent", "MouseEvent", "KeyEvent", "InputMethodEvent",
            "ScrollEvent", "DragEvent", "WindowEvent", "Event",
            "InputEvent", "ContextMenuEvent", "GestureEvent",
            "TouchEvent", "SwipeEvent", "ZoomEvent", "RotateEvent",
            "WorkerStateEvent", "MediaErrorEvent"
    );

    // Types de composants JavaFX a exclure (parfois passes en param dans les handlers FXML)
    private static final Set<String> JAVAFX_UI_TYPES = Set.of(
            "Node", "Button", "Label", "TextField", "TextArea",
            "ComboBox", "ListView", "TableView", "TreeView",
            "MenuItem", "CheckBox", "RadioButton", "ToggleButton",
            "Slider", "Spinner", "DatePicker", "ColorPicker",
            "Stage", "Scene", "Window"
    );

    private JavaFxUiTypeFilter() {}

    /**
     * Retourne true si le type est un type JavaFX UI (evenement ou composant).
     * Utilise le nom simple (sans package).
     */
    public static boolean isJavaFxUiType(final String typeName) {
        if (typeName == null || typeName.isBlank()) return false;
        // Extraire le nom simple si FQN (ex: javafx.event.ActionEvent -> ActionEvent)
        String simple = typeName.contains(".")
                ? typeName.substring(typeName.lastIndexOf('.') + 1)
                : typeName;
        return JAVAFX_EVENT_TYPES.contains(simple) || JAVAFX_UI_TYPES.contains(simple);
    }

    /**
     * Filtre les parametres JavaFX UI d'une signature.
     * Retourne une nouvelle MethodSignature sans ces parametres.
     * Retourne la signature originale si aucun filtrage n'est necessaire.
     */
    public static MethodSignature filterForDomain(final MethodSignature sig) {
        if (sig == null) return null;
        List<MethodParameter> filtered = sig.parameters().stream()
                .filter(p -> !isJavaFxUiType(p.type()))
                .toList();
        if (filtered.size() == sig.parameters().size()) {
            return sig; // aucun filtrage
        }
        return MethodSignature.of(sig.returnType(), filtered);
    }
}
