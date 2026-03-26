package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.MethodParameter;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Methodes utilitaires partagees entre tous les generateurs d'artefacts.
 * Classe finale avec uniquement des methodes statiques — pas d'instanciation.
 */
public final class GeneratorUtils {

    private static final Logger LOG = LoggerFactory.getLogger(GeneratorUtils.class);

    private GeneratorUtils() {
        // utilitaire statique — pas d'instanciation
    }

    public static String extractBaseName(final String controllerRef) {
        if (controllerRef == null || controllerRef.isBlank()) {
            return "Default";
        }
        String fileName = controllerRef;
        int lastSlash = Math.max(controllerRef.lastIndexOf('/'), controllerRef.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            fileName = controllerRef.substring(lastSlash + 1);
        }
        if (fileName.endsWith(".java")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }
        if (fileName.endsWith("Controller")) {
            fileName = fileName.substring(0, fileName.length() - "Controller".length());
        }
        return fileName.isBlank() ? "Default" : fileName;
    }

    public static String extractPackage(final String javaContent) {
        if (javaContent == null || javaContent.isBlank()) {
            return null;
        }
        for (String line : javaContent.split("\n", 30)) {
            String t = line.trim();
            if (t.startsWith("package ") && t.endsWith(";")) {
                return t.substring(8, t.length() - 1);
            }
            if (!t.isEmpty() && !t.startsWith("//") && !t.startsWith("/*") && !t.startsWith("*")) {
                break;
            }
        }
        return null;
    }

    public static void addPackage(final StringBuilder sb, final String pkg) {
        if (pkg != null && !pkg.isBlank()) {
            sb.append("package ").append(pkg).append(".migration;\n\n");
        }
    }

    public static String artifactId(final String baseName, final int lotNumber, final ArtifactType type) {
        return baseName + "-lot" + lotNumber + "-" + type.name().toLowerCase();
    }

    public static CodeArtifact artifact(
            final String baseName,
            final int lot,
            final ArtifactType type,
            final String className,
            final String content,
            final boolean bridge) {
        return new CodeArtifact(
                baseName + "-lot" + lot + "-" + type.name().toLowerCase(),
                type, lot, className, content, bridge);
    }

    /**
     * Extrait un nom de methode depuis la description d'une regle.
     *
     * Patterns reconnus (dans l'ordre) :
     * 1. "Methode handler onValueChanged : ..." -> "onValueChanged"
     * 2. "Service injecte FooService fooService : ..." -> "fooService"
     * 3. "Champ FXML Type fieldName : ..." -> "fieldName"
     * 4. "void foo(Arg arg)" -> "foo"
     * 5. Fallback camelCase sur les 3 premiers mots significatifs
     *
     * <p>JAS-008 : le nettoyage des prefixes et suffixes techniques est disponible via
     * {@link #cleanMethodName(String)} et s'applique explicitement dans les generateurs
     * qui veulent un nom semantique cible. L'extraction conserve le nom d'origine pour
     * preserver les handlers JavaFX et les signatures detectees.
     */
    public static String methodNameFromRule(final BusinessRule rule) {
        String desc = rule.description();

        // Pattern 1 — handler explicite
        if (desc.startsWith("Methode handler ")) {
            String rest = desc.substring("Methode handler ".length());
            int colon = rest.indexOf(':');
            String name = (colon > 0 ? rest.substring(0, colon) : rest).trim();
            if (isValidIdentifier(name)) {
                return name;
            }
        }

        // Pattern 1b — JAS-020 : methode garde booléenne
        if (desc.startsWith("Methode garde ")) {
            String rest = desc.substring("Methode garde ".length());
            int colon = rest.indexOf(':');
            String name = (colon > 0 ? rest.substring(0, colon) : rest).trim();
            if (isValidIdentifier(name)) {
                return name;
            }
        }

        // Pattern 2 — service injecte -> nom du champ (2eme token)
        if (desc.startsWith("Service injecte ")) {
            String rest = desc.substring("Service injecte ".length());
            int colon = rest.indexOf(':');
            String[] parts = (colon > 0 ? rest.substring(0, colon) : rest).trim().split("\\s+");
            if (parts.length >= 2 && isValidIdentifier(parts[1])) {
                return parts[1];
            }
            if (parts.length >= 1 && isValidIdentifier(parts[0])) {
                return decapitalize(parts[0]);
            }
        }

        // Pattern 3 — champ FXML -> nom du champ (dernier token avant ':')
        if (desc.startsWith("Champ FXML ")) {
            String rest = desc.substring("Champ FXML ".length());
            int colon = rest.indexOf(':');
            String[] parts = (colon > 0 ? rest.substring(0, colon) : rest).trim().split("\\s+");
            if (parts.length >= 2) {
                String name = parts[parts.length - 1];
                if (isValidIdentifier(name)) {
                    return name;
                }
            }
        }

        // Pattern 4 — signature Java avec parenthese
        int parenIndex = desc.indexOf('(');
        if (parenIndex > 0) {
            int spaceIndex = desc.lastIndexOf(' ', parenIndex);
            String name = spaceIndex >= 0
                    ? desc.substring(spaceIndex + 1, parenIndex).trim()
                    : desc.substring(0, parenIndex).trim();
            if (isValidIdentifier(name)) {
                return name;
            }
        }

        // Fallback — camelCase des 3 premiers mots sans ponctuation
        String[] words = desc.replaceAll("[^a-zA-Z0-9 ]", " ").trim().split("\\s+");
        var method = new StringBuilder();
        int added = 0;
        for (String w : words) {
            if (w.isBlank() || w.length() < 2) {
                continue;
            }
            if (added == 0) {
                method.append(w.substring(0, 1).toLowerCase()).append(w.substring(1));
            } else {
                method.append(w.substring(0, 1).toUpperCase()).append(w.substring(1));
            }
            if (++added == 3) {
                break;
            }
        }
        String result = method.toString();
        return result.isEmpty() ? "handle" : result;
    }

    /**
     * JAS-008 — Nettoie un nom de methode en supprimant les prefixes et suffixes techniques.
     * Si le resultat serait vide ou de moins de 3 caracteres, retourne le nom original.
     *
     * <p>Prefixes supprimes (dans l'ordre, insensibles a la casse, une seule iteration) :
     * on, handle/handler, btn/button, action.
     *
     * <p>Suffixes supprimes (insensibles a la casse) :
     * Clicked, Click, Action, Pressed, Press, Released, Changed, Change,
     * Selected, Select, Fired, Event, Handler.
     */
    public static String cleanMethodName(final String rawName) {
        if (rawName == null || rawName.length() < 3) {
            return rawName;
        }
        String cleaned = stripPrefix(rawName);
        cleaned = stripSuffix(cleaned);
        cleaned = decapitalize(cleaned);
        if (cleaned.isEmpty() || cleaned.length() < 3) {
            return decapitalize(rawName);
        }
        return cleaned;
    }

    private static String stripPrefix(final String name) {
        // Ordre important : prefixes les plus longs d'abord pour eviter les correspondances partielles
        String[] prefixes = {"handler", "handle", "button", "bouton", "action", "on", "btn"};
        for (String prefix : prefixes) {
            if (name.length() > prefix.length()
                    && name.toLowerCase().startsWith(prefix.toLowerCase())) {
                String remainder = name.substring(prefix.length());
                // Le reste doit commencer par une majuscule pour etre un nom camelCase valide
                if (!remainder.isEmpty() && Character.isUpperCase(remainder.charAt(0))) {
                    return remainder;
                }
            }
        }
        return name;
    }

    private static String stripSuffix(final String name) {
        String[] suffixes = {
            "Clicked", "Click", "Pressed", "Press", "Released",
            "Changed", "Change", "Selected", "Select",
            "Action", "Fired", "Event", "Handler"
        };
        for (String suffix : suffixes) {
            if (name.length() > suffix.length()
                    && name.toLowerCase().endsWith(suffix.toLowerCase())) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }

    /**
     * Nom de champ pour le ViewModel (meme logique que methodNameFromRule
     * mais sans transformation camelCase supplementaire).
     */
    public static String fieldNameFromRule(final BusinessRule rule) {
        String desc = rule.description();

        // Champ FXML -> nom du champ
        if (desc.startsWith("Champ FXML ")) {
            String rest = desc.substring("Champ FXML ".length());
            int colon = rest.indexOf(':');
            String[] parts = (colon > 0 ? rest.substring(0, colon) : rest).trim().split("\\s+");
            if (parts.length >= 2) {
                String name = parts[parts.length - 1];
                if (isValidIdentifier(name)) {
                    return name;
                }
            }
        }

        // Methode handler -> nom de la methode
        if (desc.startsWith("Methode handler ")) {
            String rest = desc.substring("Methode handler ".length());
            int colon = rest.indexOf(':');
            String name = (colon > 0 ? rest.substring(0, colon) : rest).trim();
            if (isValidIdentifier(name)) {
                return name;
            }
        }

        // Fallback sur methodNameFromRule
        String method = methodNameFromRule(rule);
        if (method.startsWith("handle") && method.length() > 6) {
            return decapitalize(method.substring(6));
        }
        return method;
    }

    /**
     * Extrait le type JavaFX du champ depuis une description "Champ FXML TypeName fieldName :".
     * Retourne une chaine vide si non applicable.
     */
    public static String extractFxmlType(final String description) {
        if (!description.startsWith("Champ FXML ")) {
            return "";
        }
        String rest = description.substring("Champ FXML ".length());
        int colon = rest.indexOf(':');
        String[] parts = (colon > 0 ? rest.substring(0, colon) : rest).trim().split("\\s+");
        return parts.length >= 1 ? parts[0] : "";
    }

    /**
     * Mappe un type de composant JavaFX a une propriete semantique du ViewModel.
     * Retourne null pour les composants dont la representation est trop complexe (TableView...).
     *
     * <p>JAS-007 : apres la correspondance exacte, une heuristique par sous-chaine (case-insensitive)
     * resout les types custom heritant de composants JavaFX standards.
     */
    public static ViewModelProperty fxmlTypeToProperty(final String rawField, final String fxmlType) {
        String lower = fxmlType.toLowerCase();

        // Correspondance exacte — types standards JavaFX
        if (lower.endsWith("vbox") || lower.endsWith("hbox") || lower.endsWith("pane")
                || lower.endsWith("gridpane") || lower.endsWith("flowpane")
                || lower.endsWith("borderpane") || lower.endsWith("anchorpane")
                || lower.endsWith("stackpane") || lower.endsWith("splitpane")) {
            return new ViewModelProperty(rawField + "Visible", PropertyType.BOOLEAN);
        }
        if (lower.endsWith("button") || lower.endsWith("btn") || lower.endsWith("menuitem")
                || lower.endsWith("hyperlink")) {
            return new ViewModelProperty(rawField + "Enabled", PropertyType.BOOLEAN);
        }
        if (lower.endsWith("checkbox") || lower.endsWith("chcbx")
                || lower.endsWith("radiobutton") || lower.endsWith("togglebutton")) {
            return new ViewModelProperty(rawField + "Selected", PropertyType.BOOLEAN);
        }
        if (lower.endsWith("label") || lower.endsWith("lbl")
                || lower.endsWith("textfield") || lower.endsWith("textarea")
                || lower.endsWith("passwordfield")) {
            return new ViewModelProperty(rawField + "Text", PropertyType.STRING);
        }
        if (lower.endsWith("combobox") || lower.endsWith("choicebox")
                || lower.endsWith("spinner") || lower.endsWith("slider")) {
            return new ViewModelProperty(rawField + "Value", PropertyType.STRING);
        }
        if (lower.endsWith("tableview") || lower.endsWith("listview")
                || lower.endsWith("treeview")) {
            // JAS-007 : les vues liste exposent une ObservableList<Object> plutot qu'un TODO
            return new ViewModelProperty(rawField + "Items", PropertyType.OBSERVABLE_LIST);
        }

        // JAS-007 — heuristique par sous-chaine pour les types custom heritant de composants JavaFX
        ViewModelProperty heuristicResult = resolveByHeuristic(rawField, fxmlType, lower);
        if (heuristicResult != null) {
            return heuristicResult;
        }

        return new ViewModelProperty(rawField, inferPropertyType(rawField));
    }

    /**
     * JAS-007 — Resout un type custom JavaFX par heuristique de sous-chaine (case-insensitive).
     * Retourne null si aucune heuristique ne correspond (type inconnu).
     */
    private static ViewModelProperty resolveByHeuristic(
            final String rawField, final String fxmlType, final String lower) {
        // TableView avant ListView et TreeView pour eviter les correspondances "table" ambigues
        // JAS-007 : les types custom heritant de TableView/ListView/TreeView generent une ObservableList
        if (lower.contains("tableview") || lower.contains("table")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'TableView'", fxmlType);
            return new ViewModelProperty(rawField + "Items", PropertyType.OBSERVABLE_LIST);
        }
        if (lower.contains("listview") || lower.contains("list")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'ListView'", fxmlType);
            return new ViewModelProperty(rawField + "Items", PropertyType.OBSERVABLE_LIST);
        }
        if (lower.contains("treeview") || lower.contains("tree")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'TreeView'", fxmlType);
            return new ViewModelProperty(rawField + "Items", PropertyType.OBSERVABLE_LIST);
        }
        if (lower.contains("gridpane") || lower.contains("grid")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'GridPane'", fxmlType);
            return new ViewModelProperty(rawField + "Visible", PropertyType.BOOLEAN);
        }
        if (lower.contains("checkbox") || lower.contains("toggle") || lower.contains("radio")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'CheckBox'", fxmlType);
            return new ViewModelProperty(rawField + "Selected", PropertyType.BOOLEAN);
        }
        if (lower.contains("button") || lower.contains("btn") || lower.contains("bouton")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'Button'", fxmlType);
            return new ViewModelProperty(rawField + "Enabled", PropertyType.BOOLEAN);
        }
        if (lower.contains("label") || lower.contains("lbl")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'Label'", fxmlType);
            return new ViewModelProperty(rawField + "Text", PropertyType.STRING);
        }
        if (lower.contains("textfield") || lower.contains("textarea")
                || lower.contains("input") || lower.contains("champ")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'TextField'", fxmlType);
            return new ViewModelProperty(rawField + "Text", PropertyType.STRING);
        }
        // "text" seul apres les variantes plus specifiques
        if (lower.contains("text")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'TextField'", fxmlType);
            return new ViewModelProperty(rawField + "Text", PropertyType.STRING);
        }
        if (lower.contains("combobox") || lower.contains("combo") || lower.contains("choice")
                || lower.contains("spinner") || lower.contains("slider")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'ComboBox'", fxmlType);
            return new ViewModelProperty(rawField + "Value", PropertyType.STRING);
        }
        // "tab" sans "table" (deja traite plus haut)
        if (lower.contains("tab")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'TabPane'", fxmlType);
            return new ViewModelProperty(rawField + "Visible", PropertyType.BOOLEAN);
        }
        if (lower.contains("vbox") || lower.contains("hbox") || lower.contains("pane")
                || lower.contains("box") || lower.contains("container") || lower.contains("panel")) {
            LOG.debug("[JAS-007] Type custom '{}' resolu par heuristique vers 'VBox'", fxmlType);
            return new ViewModelProperty(rawField + "Visible", PropertyType.BOOLEAN);
        }
        return null;
    }

    /** Infere le type JavaFX le plus approprie selon la convention de nommage du champ. */
    public static PropertyType inferPropertyType(final String field) {
        String lower = field.toLowerCase();
        if (lower.startsWith("is") || lower.startsWith("has") || lower.startsWith("can")
                || lower.startsWith("show") || lower.endsWith("visible")
                || lower.endsWith("enabled") || lower.endsWith("selected")) {
            return PropertyType.BOOLEAN;
        }
        if (lower.startsWith("count") || lower.startsWith("index") || lower.startsWith("size")
                || lower.startsWith("nb") || lower.endsWith("count") || lower.endsWith("index")) {
            return PropertyType.INTEGER;
        }
        return PropertyType.STRING;
    }

    public static boolean isValidIdentifier(final String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String capitalize(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String decapitalize(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Retourne la liste de parametres Java pour une regle donnee.
     * Si la regle possede une signature AST, les parametres sont extraits fidelement.
     * Si la signature est absente (mode regex fallback), retourne une chaine vide.
     */
    public static String buildMethodSignature(final BusinessRule rule) {
        if (!rule.hasSignature()) {
            return "";
        }
        MethodSignature sig = rule.signature();
        return sig.toParameterList();
    }

    /**
     * Retourne le type de retour Java pour une regle donnee.
     * Si la regle possede une signature AST, le type de retour est extrait fidelement.
     * Si la signature est absente (mode regex fallback), retourne "void".
     */
    public static String buildReturnType(final BusinessRule rule) {
        if (!rule.hasSignature()) {
            return "void";
        }
        return rule.signature().returnType();
    }

    /**
     * Retourne la liste des noms de parametres (sans types) pour la delegation.
     * Exemple : "patientId, type" pour passer les args a useCase.method(patientId, type).
     * Retourne une chaine vide si aucun parametre ou signature absente.
     */
    public static String buildArgumentList(final BusinessRule rule) {
        if (!rule.hasSignature()) {
            return "";
        }
        return rule.signature().parameters().stream()
                .map(p -> p.name())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * JAS-008 — Retourne la liste de parametres filtree pour le domaine UseCase.
     * Les types JavaFX UI (ActionEvent, MouseEvent, etc.) sont exclus.
     * Si la regle n'a pas de signature, retourne une chaine vide.
     */
    public static String buildUseCaseParams(final BusinessRule rule) {
        if (!rule.hasSignature()) {
            return "";
        }
        MethodSignature filtered = JavaFxUiTypeFilter.filterForDomain(rule.signature());
        return filtered.toParameterList();
    }

    /**
     * JAS-008 — Retourne la liste d'arguments filtree pour l'appel useCase.method(args).
     * Les arguments de type JavaFX UI sont exclus pour ne pas fuiter dans le domaine.
     * Retourne une chaine vide si aucun argument ou signature absente.
     */
    public static String buildUseCaseArgList(final BusinessRule rule) {
        if (!rule.hasSignature()) {
            return "";
        }
        MethodSignature filtered = JavaFxUiTypeFilter.filterForDomain(rule.signature());
        return filtered.parameters().stream()
                .map(MethodParameter::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * JAS-008 — Collecte les types non-standards des signatures de regles.
     * Exclut : primitives, java.lang.*, types JavaFX UI, types connus du JDK.
     * Retourne une liste de noms de types pour lesquels un import est necessaire.
     */
    public static List<String> collectTypeHints(final List<BusinessRule> rules) {
        Set<String> seen = new java.util.LinkedHashSet<>();
        Set<String> known = Set.of(
                "void", "boolean", "int", "long", "double", "float", "short", "byte", "char",
                "Boolean", "Integer", "Long", "Double", "Float", "String", "Object",
                "List", "Map", "Set", "Optional", "Collection",
                "LocalDate", "LocalDateTime", "Instant", "BigDecimal"
        );
        for (BusinessRule rule : rules) {
            if (!rule.hasSignature()) continue;
            MethodSignature sig = JavaFxUiTypeFilter.filterForDomain(rule.signature());
            for (MethodParameter p : sig.parameters()) {
                String type = p.type();
                // Supprimer les generics : List<Patient> -> List
                int lt = type.indexOf('<');
                String simple = lt > 0 ? type.substring(0, lt) : type;
                if (!known.contains(simple) && !JavaFxUiTypeFilter.isJavaFxUiType(simple) && !p.unknown()) {
                    seen.add(simple);
                }
            }
            // Type de retour non-void
            String rt = sig.returnType();
            if (!"void".equals(rt)) {
                int lt = rt.indexOf('<');
                String simple = lt > 0 ? rt.substring(0, lt) : rt;
                if (!known.contains(simple) && !JavaFxUiTypeFilter.isJavaFxUiType(simple)) {
                    seen.add(simple);
                }
            }
        }
        return List.copyOf(seen);
    }
}
