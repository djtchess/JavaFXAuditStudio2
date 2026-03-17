package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse.BusinessRuleDto;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;

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
                result.parsingFallbackReason());
    }

    private List<BusinessRuleDto> buildAllRules(final ClassificationResult result) {
        List<BusinessRuleDto> allRules = new ArrayList<>();
        result.rules().stream().map(this::toDto).forEach(allRules::add);
        result.uncertainRules().stream().map(this::toDto).forEach(allRules::add);
        return List.copyOf(allRules);
    }

    private BusinessRuleDto toDto(final BusinessRule rule) {
        return new BusinessRuleDto(
                rule.ruleId(),
                rule.description(),
                rule.responsibilityClass().name(),
                rule.extractionCandidate().name(),
                rule.uncertain());
    }
}
