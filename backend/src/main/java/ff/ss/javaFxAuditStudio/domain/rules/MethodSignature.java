package ff.ss.javaFxAuditStudio.domain.rules;

import java.util.List;
import java.util.Objects;

/**
 * Signature complete d'une methode extraite d'un controller JavaFX.
 *
 * @param returnType  type de retour Java (ex: "void", "PatientDto", "List")
 * @param parameters  liste ordonnee des parametres
 * @param hasUnknowns vrai si au moins un type n'a pas pu etre resolu
 */
public record MethodSignature(String returnType, List<MethodParameter> parameters, boolean hasUnknowns) {

    public MethodSignature {
        Objects.requireNonNull(returnType, "returnType must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        parameters = List.copyOf(parameters);
    }

    public static MethodSignature voidNoArgs() {
        return new MethodSignature("void", List.of(), false);
    }

    public static MethodSignature of(final String returnType, final List<MethodParameter> parameters) {
        boolean hasUnknowns = parameters.stream().anyMatch(MethodParameter::unknown);
        return new MethodSignature(returnType, parameters, hasUnknowns);
    }

    /**
     * Genere la liste de parametres Java : "final Long patientId, final ExamenType type"
     * Retourne une chaine vide si aucun parametre.
     */
    public String toParameterList() {
        if (parameters.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("final ").append(parameters.get(i).toJavaDeclaration());
        }
        return sb.toString();
    }
}
