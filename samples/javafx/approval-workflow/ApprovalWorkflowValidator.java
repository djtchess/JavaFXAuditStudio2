package app.ui;

public class ApprovalWorkflowValidator {

    public boolean validateComment(String comment) {
        return comment != null && !comment.isBlank() && comment.trim().length() >= 10;
    }
}
