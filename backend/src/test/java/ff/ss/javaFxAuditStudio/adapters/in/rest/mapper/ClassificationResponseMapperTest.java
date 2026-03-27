package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse.BusinessRuleDto;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse.MethodSignatureDto;
import ff.ss.javaFxAuditStudio.domain.analysis.ControllerDependency;
import ff.ss.javaFxAuditStudio.domain.analysis.DeltaAnalysisSummary;
import ff.ss.javaFxAuditStudio.domain.analysis.DependencyKind;
import ff.ss.javaFxAuditStudio.domain.analysis.DetectionStatus;
import ff.ss.javaFxAuditStudio.domain.analysis.StateMachineInsight;
import ff.ss.javaFxAuditStudio.domain.analysis.StateTransition;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.MethodParameter;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;
import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

/**
 * Tests unitaires du mapper ClassificationResponseMapper.
 * Verifie que les signatures de methodes sont correctement mappees vers les DTOs REST.
 */
class ClassificationResponseMapperTest {

    private ClassificationResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ClassificationResponseMapper();
    }

    // -------------------------------------------------------------------------
    // Regle avec signature : le DTO doit contenir une MethodSignatureDto non-null
    // -------------------------------------------------------------------------

    @Test
    void toResponse_ruleAvecSignature_producesBusinessRuleDtoWithSignatureNonNull() {
        MethodSignature signature;
        BusinessRule regle;
        ClassificationResult result;
        ClassificationResponse response;
        BusinessRuleDto ruleDto;
        MethodSignatureDto signatureDto;

        // Construction d'une signature avec un parametre connu
        signature = MethodSignature.of(
                "void",
                List.of(MethodParameter.known("Long", "patientId")));

        regle = new BusinessRule(
                "RG-001",
                "Valider le patient avant enregistrement",
                "PatientController.java",
                42,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                false,
                signature);

        result = new ClassificationResult(
                "PatientController",
                List.of(regle),
                List.of(),
                ParsingMode.AST,
                null,
                0);

        response = mapper.toResponse(result);

        assertThat(response.rules()).hasSize(1);
        ruleDto = response.rules().get(0);

        // La signature doit etre presente
        assertThat(ruleDto.signature()).isNotNull();
        signatureDto = ruleDto.signature();

        assertThat(signatureDto.returnType()).isEqualTo("void");
        assertThat(signatureDto.hasUnknowns()).isFalse();
        assertThat(signatureDto.parameters()).hasSize(1);
        assertThat(signatureDto.parameters().get(0).type()).isEqualTo("Long");
        assertThat(signatureDto.parameters().get(0).name()).isEqualTo("patientId");
        assertThat(signatureDto.parameters().get(0).unknown()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Regle sans signature : le DTO doit contenir une MethodSignatureDto null
    // -------------------------------------------------------------------------

    @Test
    void toResponse_regleSansSignature_producesBusinessRuleDtoWithSignatureNull() {
        BusinessRule regle;
        ClassificationResult result;
        ClassificationResponse response;
        BusinessRuleDto ruleDto;

        // Construction d'une regle sans signature (constructeur 7-args)
        regle = new BusinessRule(
                "RG-002",
                "Afficher le formulaire de creation",
                "PatientController.java",
                10,
                ResponsibilityClass.UI,
                ExtractionCandidate.NONE,
                false);

        result = new ClassificationResult(
                "PatientController",
                List.of(regle),
                List.of(),
                ParsingMode.REGEX_FALLBACK,
                "AST indisponible pour ce fichier",
                0);

        response = mapper.toResponse(result);

        assertThat(response.rules()).hasSize(1);
        ruleDto = response.rules().get(0);

        // La signature doit etre null en mode regex fallback
        assertThat(ruleDto.signature()).isNull();
    }

    // -------------------------------------------------------------------------
    // Signature avec parametre inconnu : hasUnknowns doit etre vrai dans le DTO
    // -------------------------------------------------------------------------

    @Test
    void toResponse_signatureAvecParametreInconnu_setsHasUnknownsTrue() {
        MethodSignature signature;
        BusinessRule regle;
        ClassificationResult result;
        ClassificationResponse response;
        MethodSignatureDto signatureDto;

        // Parametre dont le type n'a pas pu etre resolu (mode regex)
        signature = MethodSignature.of(
                "String",
                List.of(MethodParameter.unknown("param0")));

        regle = new BusinessRule(
                "RG-003",
                "Calculer le code diagnostic",
                "PatientController.java",
                77,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                true,
                signature);

        result = new ClassificationResult(
                "PatientController",
                List.of(),
                List.of(regle),
                ParsingMode.AST,
                null,
                0);

        response = mapper.toResponse(result);

        assertThat(response.rules()).hasSize(1);
        signatureDto = response.rules().get(0).signature();

        assertThat(signatureDto).isNotNull();
        assertThat(signatureDto.hasUnknowns()).isTrue();
        assertThat(signatureDto.parameters().get(0).unknown()).isTrue();
        assertThat(signatureDto.parameters().get(0).type()).isEqualTo("Object");
    }

    // -------------------------------------------------------------------------
    // Champs de base du DTO de regle (controllerRef, parsingMode, ruleId…)
    // -------------------------------------------------------------------------

    @Test
    void toResponse_mapsBaseFieldsCorrectly() {
        BusinessRule regle;
        ClassificationResult result;
        ClassificationResponse response;
        BusinessRuleDto ruleDto;

        regle = new BusinessRule(
                "RG-004",
                "Verifier les droits avant suppression",
                "PatientController.java",
                55,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);

        result = new ClassificationResult(
                "PatientController",
                List.of(regle),
                List.of(),
                ParsingMode.AST,
                null,
                2);

        response = mapper.toResponse(result);

        assertThat(response.controllerRef()).isEqualTo("PatientController");
        assertThat(response.parsingMode()).isEqualTo("AST");
        assertThat(response.parsingFallbackReason()).isNull();
        assertThat(response.excludedLifecycleMethodsCount()).isEqualTo(2);
        assertThat(response.ruleCount()).isEqualTo(1);
        assertThat(response.uncertainCount()).isEqualTo(0);

        ruleDto = response.rules().get(0);
        assertThat(ruleDto.ruleId()).isEqualTo("RG-004");
        assertThat(ruleDto.description()).isEqualTo("Verifier les droits avant suppression");
        assertThat(ruleDto.responsibilityClass()).isEqualTo("APPLICATION");
        assertThat(ruleDto.extractionCandidate()).isEqualTo("USE_CASE");
        assertThat(ruleDto.uncertain()).isFalse();
        assertThat(ruleDto.signature()).isNull();
    }

    @Test
    void toResponse_mapsAdvancedAnalysisFields() {
        BusinessRule regle;
        ClassificationResult result;
        ClassificationResponse response;

        regle = new BusinessRule(
                "RG-005",
                "Methode handler onValidate : validation metier",
                "PatientController.java",
                88,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);

        result = new ClassificationResult(
                "PatientController",
                List.of(regle),
                List.of(),
                ParsingMode.AST,
                null,
                1,
                new StateMachineInsight(
                        DetectionStatus.CONFIRMED,
                        0.82d,
                        List.of("DRAFT", "VALIDATED"),
                        List.of(new StateTransition("DRAFT", "VALIDATED", "onValidate"))),
                List.of(new ControllerDependency(
                        DependencyKind.SHARED_SERVICE,
                        "BillingService",
                        "billingService")),
                new DeltaAnalysisSummary(1, 2, 3));

        response = mapper.toResponse(result);

        assertThat(response.stateMachine().status()).isEqualTo("CONFIRMED");
        assertThat(response.stateMachine().confidence()).isEqualTo(0.82d);
        assertThat(response.stateMachine().states()).containsExactly("DRAFT", "VALIDATED");
        assertThat(response.stateMachine().transitions()).hasSize(1);
        assertThat(response.stateMachine().transitions().get(0).fromState()).isEqualTo("DRAFT");
        assertThat(response.dependencies()).hasSize(1);
        assertThat(response.dependencies().get(0).kind()).isEqualTo("SHARED_SERVICE");
        assertThat(response.dependencies().get(0).target()).isEqualTo("BillingService");
        assertThat(response.deltaAnalysis().addedRules()).isEqualTo(1);
        assertThat(response.deltaAnalysis().removedRules()).isEqualTo(2);
        assertThat(response.deltaAnalysis().changedRules()).isEqualTo(3);
        assertThat(response.deltaAnalysis().hasChanges()).isTrue();
    }
}
