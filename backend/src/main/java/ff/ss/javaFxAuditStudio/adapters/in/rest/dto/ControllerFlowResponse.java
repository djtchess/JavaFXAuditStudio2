package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Schema(description = "Analyse de flux controller : state machine, gardes policy et signaux UI")
public record ControllerFlowResponse(
        @Schema(description = "Reference du controller")
        String controllerRef,
        @Schema(description = "Nom simple du controller")
        String controllerName,
        @Schema(description = "Vrai si une state machine a ete detectee")
        boolean stateMachineDetected,
        @Schema(description = "Confiance de la detection entre 0 et 1")
        double stateMachineConfidence,
        @Schema(description = "Niveau de detection (CONFIRMED, POSSIBLE, NONE)")
        String detectionLevel,
        @Schema(description = "Liste des etats identifiables")
        List<String> states,
        @Schema(description = "Liste des transitions identifiees")
        List<StateTransitionDto> transitions,
        @Schema(description = "Noms des methodes gardes policy")
        List<String> policyGuardCandidates,
        @Schema(description = "Noms des methodes purement UI exclues des policies")
        List<String> uiGuardMethods,
        @Schema(description = "Signaux d'analyse utilises pour le diagnostic")
        List<String> evidence,
        @Schema(description = "Avertissements")
        List<String> warnings) {

    public ControllerFlowResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(controllerName, "controllerName must not be null");
        Objects.requireNonNull(detectionLevel, "detectionLevel must not be null");
        Objects.requireNonNull(states, "states must not be null");
        Objects.requireNonNull(transitions, "transitions must not be null");
        Objects.requireNonNull(policyGuardCandidates, "policyGuardCandidates must not be null");
        Objects.requireNonNull(uiGuardMethods, "uiGuardMethods must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        states = List.copyOf(states);
        transitions = List.copyOf(transitions);
        policyGuardCandidates = List.copyOf(policyGuardCandidates);
        uiGuardMethods = List.copyOf(uiGuardMethods);
        evidence = List.copyOf(evidence);
        warnings = List.copyOf(warnings);
    }

    @Schema(description = "Transition detectee entre deux etats")
    public record StateTransitionDto(
            @Schema(description = "Etat source")
            String sourceState,
            @Schema(description = "Etat cible")
            String targetState,
            @Schema(description = "Methode declenchante")
            String triggerMethod,
            @Schema(description = "Expression de garde")
            String guardExpression,
            @Schema(description = "Numero de ligne source")
            int sourceLine) {

        public StateTransitionDto {
            Objects.requireNonNull(sourceState, "sourceState must not be null");
            Objects.requireNonNull(targetState, "targetState must not be null");
            Objects.requireNonNull(triggerMethod, "triggerMethod must not be null");
            Objects.requireNonNull(guardExpression, "guardExpression must not be null");
        }
    }
}
