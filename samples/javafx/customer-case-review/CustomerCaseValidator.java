package app.ui.customer;

import java.math.BigDecimal;

public final class CustomerCaseValidator {
    private static final BigDecimal EXPRESS_MAX_AMOUNT = new BigDecimal("5000");

    public ValidationResult validate(CustomerCaseForm form) {
        if (!form.kycCompleted() || !form.documentsComplete()) {
            return ValidationResult.failure("RG-07: la validation exige un KYC complet et des documents complets.");
        }
        if (form.fraudAlert() || form.complianceBlock()) {
            return ValidationResult.failure("RG-03: un dossier bloque par fraude ou conformite ne peut pas etre valide.");
        }
        if (form.expressMode() && form.requestedAmount().compareTo(EXPRESS_MAX_AMOUNT) > 0) {
            return ValidationResult.failure("RG-08: le mode express est interdit au-dela de 5000.");
        }
        if ("PREMIUM".equals(form.caseType()) && !form.premiumCustomer()) {
            return ValidationResult.failure("RG-09: un dossier premium exige la confirmation du statut premium.");
        }
        if (form.manualReview() && form.manualReviewReason().isBlank()) {
            return ValidationResult.failure("RG-10: la revue manuelle exige une justification.");
        }
        return ValidationResult.success();
    }

    public record CustomerCaseForm(
            String caseType,
            BigDecimal requestedAmount,
            boolean premiumCustomer,
            boolean kycCompleted,
            boolean documentsComplete,
            boolean fraudAlert,
            boolean complianceBlock,
            boolean expressMode,
            boolean manualReview,
            String manualReviewReason
    ) {
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }
}
