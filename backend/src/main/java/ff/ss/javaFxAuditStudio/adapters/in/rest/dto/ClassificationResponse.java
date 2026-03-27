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
        int excludedLifecycleMethodsCount,
        @Schema(description = "Logique d'etat detectee dans le controller")
        StateMachineDto stateMachine,
        @Schema(description = "Dependances detectees autour du controller")
        List<DependencyDto> dependencies,
        @Schema(description = "Resume differentiel entre le cache et le source courant")
        DeltaAnalysisDto deltaAnalysis) {

    public ClassificationResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(parsingMode, "parsingMode must not be null");
        Objects.requireNonNull(stateMachine, "stateMachine must not be null");
        Objects.requireNonNull(dependencies, "dependencies must not be null");
        Objects.requireNonNull(deltaAnalysis, "deltaAnalysis must not be null");
        rules = List.copyOf(rules);
        dependencies = List.copyOf(dependencies);
        // parsingFallbackReason peut etre null si mode AST
    }

    @Schema(description = "Transition de logique d'etat detectee")
    public record StateTransitionDto(
            @Schema(description = "Etat source")
            String fromState,
            @Schema(description = "Etat cible")
            String toState,
            @Schema(description = "Methode declenchante")
            String trigger) {}

    @Schema(description = "Logique d'etat detectee dans le controller")
    public record StateMachineDto(
            @Schema(description = "Statut de detection (ABSENT, POSSIBLE, CONFIRMED)")
            String status,
            @Schema(description = "Niveau de confiance entre 0.0 et 1.0")
            double confidence,
            @Schema(description = "Etats identifies")
            List<String> states,
            @Schema(description = "Transitions detectees")
            List<StateTransitionDto> transitions) {}

    @Schema(description = "Dependance detectee autour du controller")
    public record DependencyDto(
            @Schema(description = "Nature de la dependance")
            String kind,
            @Schema(description = "Cible de la dependance")
            String target,
            @Schema(description = "Point de detection")
            String via) {}

    @Schema(description = "Resume differentiel entre le cache et le source courant")
    public record DeltaAnalysisDto(
            @Schema(description = "Nombre de regles ajoutees")
            int addedRules,
            @Schema(description = "Nombre de regles supprimees")
            int removedRules,
            @Schema(description = "Nombre de regles recategorisees")
            int changedRules,
            @Schema(description = "Vrai si le source courant diverge du cache")
            boolean hasChanges) {}

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
