package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse.BusinessRuleDto;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse.MethodParameterDto;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse.MethodSignatureDto;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;

@Component
public class ClassificationResponseMapper {

    public ClassificationResponse toResponse(final ClassificationResult result) {
        List<BusinessRuleDto> allRules = buildAllRules(result);
        String parsingMode = result.parsingMode() != null
                ? result.parsingMode().name()
                : "AST";
        return new ClassificationResponse(
                result.controllerRef(),
                result.rules().size(),
                result.uncertainRules().size(),
                allRules,
                parsingMode,
                result.parsingFallbackReason(),
                result.excludedLifecycleMethodsCount());
    }

    private List<BusinessRuleDto> buildAllRules(final ClassificationResult result) {
        List<BusinessRuleDto> allRules = new ArrayList<>();
        result.rules().stream().map(this::toDto).forEach(allRules::add);
        result.uncertainRules().stream().map(this::toDto).forEach(allRules::add);
        return List.copyOf(allRules);
    }

    private BusinessRuleDto toDto(final BusinessRule rule) {
        // La signature est null en mode regex fallback ou si l'AST ne l'a pas resolue
        MethodSignatureDto signatureDto = rule.hasSignature()
                ? toSignatureDto(rule.signature())
                : null;
        return new BusinessRuleDto(
                rule.ruleId(),
                rule.description(),
                rule.responsibilityClass().name(),
                rule.extractionCandidate().name(),
                rule.uncertain(),
                signatureDto);
    }

    /**
     * Convertit une signature de methode du domaine en DTO REST.
     */
    private MethodSignatureDto toSignatureDto(final MethodSignature sig) {
        List<MethodParameterDto> params = sig.parameters().stream()
                .map(p -> new MethodParameterDto(p.type(), p.name(), p.unknown()))
                .toList();
        return new MethodSignatureDto(sig.returnType(), params, sig.hasUnknowns());
    }
}
