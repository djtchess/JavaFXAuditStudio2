package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Regles metier classifiees extraites d'un controller JavaFX")
public record ClassificationResponse(
        @Schema(description = "Reference du controller")
        String controllerRef,
        @Schema(description = "Nombre total de regles extraites")
        int ruleCount,
        @Schema(description = "Nombre de regles avec classification incertaine")
        int uncertainCount,
        @Schema(description = "Liste des regles metier")
        List<BusinessRuleDto> rules,
        @Schema(description = "Mode d'extraction utilise (JAVA_PARSER, REGEX, etc.)")
        String parsingMode,
        @Schema(description = "Raison du fallback si mode secondaire utilise", nullable = true)
        String parsingFallbackReason,
        @Schema(description = "Methodes lifecycle exclues de l'extraction")
        int excludedLifecycleMethodsCount) {

    public ClassificationResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(parsingMode, "parsingMode must not be null");
        rules = List.copyOf(rules);
        // parsingFallbackReason peut etre null si mode AST
    }

    /**
     * DTO representant un parametre de methode extrait d'un controller JavaFX.
     *
     * @param type    type Java du parametre (ex. "Long", "String")
     * @param name    nom du parametre (ex. "patientId")
     * @param unknown vrai si le type n'a pas pu etre resolu (mode regex fallback)
     */
    @Schema(description = "Parametre d'une methode")
    public record MethodParameterDto(
            @Schema(description = "Type Java du parametre (ex: Long, String)")
            String type,
            @Schema(description = "Nom du parametre (ex: patientId)")
            String name,
            @Schema(description = "Vrai si le type n'a pas pu etre resolu en mode regex fallback")
            boolean unknown) {}

    /**
     * DTO representant la signature complete d'une methode extraite d'un controller JavaFX.
     *
     * @param returnType  type de retour Java (ex. "void", "PatientDto")
     * @param parameters  liste ordonnee des parametres
     * @param hasUnknowns vrai si au moins un type n'a pas pu etre resolu
     */
    @Schema(description = "Signature de methode extraite")
    public record MethodSignatureDto(
            @Schema(description = "Type de retour Java (ex: void, PatientDto)")
            String returnType,
            @Schema(description = "Liste ordonnee des parametres de la methode")
            List<MethodParameterDto> parameters,
            @Schema(description = "Vrai si au moins un type de parametre n'a pas pu etre resolu")
            boolean hasUnknowns) {}

    /**
     * DTO representant une regle de gestion identifiee dans un controller JavaFX.
     * Le champ signature est nullable : null en mode regex fallback, non-null en mode AST.
     */
    @Schema(description = "Regle metier extraite du controller")
    public record BusinessRuleDto(
            @Schema(description = "Identifiant unique de la regle")
            String ruleId,
            @Schema(description = "Enonce de la regle metier")
            String description,
            @Schema(description = "Categorie de responsabilite attribuee a la regle")
            String responsibilityClass,
            @Schema(description = "Candidat d'extraction recommande (nom de methode ou classe cible)")
            String extractionCandidate,
            @Schema(description = "Vrai si la classification necessite une validation humaine")
            boolean uncertain,
            @Schema(description = "Signature de la methode source, null en mode regex fallback", nullable = true)
            MethodSignatureDto signature) {

        public BusinessRuleDto {
            Objects.requireNonNull(ruleId, "ruleId must not be null");
            Objects.requireNonNull(description, "description must not be null");
            Objects.requireNonNull(responsibilityClass, "responsibilityClass must not be null");
            Objects.requireNonNull(extractionCandidate, "extractionCandidate must not be null");
            // signature peut etre null si mode regex fallback ou signature non disponible
        }
    }
}
