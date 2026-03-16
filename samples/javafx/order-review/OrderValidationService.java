package app.ui.order;

public class OrderValidationService {

    public void validateOrder(String orderNumber, String customerSegment, String reviewComment) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number is mandatory.");
        }
        if ("SENSITIVE".equalsIgnoreCase(customerSegment) && (reviewComment == null || reviewComment.isBlank())) {
            throw new IllegalStateException("Sensitive orders require a review comment.");
        }
    }
}
