package app.ui.payment;

import java.math.BigDecimal;

public class PaymentEscalationService {

    public void escalate(String paymentReference, String amount, String escalationReason) {
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException("Payment reference is mandatory.");
        }
        BigDecimal parsedAmount = new BigDecimal(amount == null || amount.isBlank() ? "0" : amount);
        if (parsedAmount.signum() <= 0) {
            throw new IllegalStateException("Escalated payment amount must be positive.");
        }
        if (parsedAmount.intValue() > 10000 && (escalationReason == null || escalationReason.isBlank())) {
            throw new IllegalStateException("Large escalations require a justification.");
        }
    }
}
