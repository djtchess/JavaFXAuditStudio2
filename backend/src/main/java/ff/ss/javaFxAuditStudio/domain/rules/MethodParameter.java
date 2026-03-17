package ff.ss.javaFxAuditStudio.domain.rules;

import java.util.Objects;

/**
 * Parametre d'une methode extraite d'un controller JavaFX.
 *
 * @param type    nom du type Java (ex: "Long", "ExamenType", "String")
 * @param name    nom du parametre (ex: "patientId", "type")
 * @param unknown vrai si le type n'a pas pu etre resolu (mode regex fallback)
 */
public record MethodParameter(String type, String name, boolean unknown) {

    public MethodParameter {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }

    public static MethodParameter known(final String type, final String name) {
        return new MethodParameter(type, name, false);
    }

    public static MethodParameter unknown(final String name) {
        return new MethodParameter("Object", name, true);
    }

    /** Representation Java : ex. "Long patientId" ou "Object param0" si type inconnu. */
    public String toJavaDeclaration() {
        return type + " " + name;
    }
}
