package app.ui.order;

public class OrderDecisionExportService {

    public void exportDecision(String orderNumber, String reviewComment) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number is mandatory.");
        }
        String payload = orderNumber + ":" + (reviewComment == null ? "" : reviewComment.trim());
        if (payload.length() < 3) {
            throw new IllegalStateException("Decision payload is too short.");
        }
    }
}
