package app.ui;

public class ApprovalTransitionHandler {

    public void handleApproval(String comment) {
        if (comment == null) {
            throw new IllegalArgumentException("comment is required");
        }
    }
}
