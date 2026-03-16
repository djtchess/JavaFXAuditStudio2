package app.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class ApprovalWorkflowController {
    @FXML
    private TextArea decisionCommentArea;
    @FXML
    private Button approveButton;

    private ApprovalWorkflowValidator approvalWorkflowValidator;
    private ApprovalTransitionHandler approvalTransitionHandler;

    public void onApprove() {
        if (!approvalWorkflowValidator.validateComment(decisionCommentArea.getText())) {
            approveButton.setDisable(true);
            return;
        }
        approvalTransitionHandler.handleApproval(decisionCommentArea.getText());
        approveButton.setVisible(false);
    }
}
