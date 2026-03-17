package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;

/**
 * Methodes utilitaires partagees entre tous les generateurs d'artefacts.
 * Classe finale avec uniquement des methodes statiques — pas d'instanciation.
 */
public final class GeneratorUtils {

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
     */
    public static ViewModelProperty fxmlTypeToProperty(final String rawField, final String fxmlType) {
        String lower = fxmlType.toLowerCase();
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
            return null;
        }
        return new ViewModelProperty(rawField, inferPropertyType(rawField));
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
}
