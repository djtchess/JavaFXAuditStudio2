package app.ui.order;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class OrderReviewController {

    @FXML
    private TextField orderNumberField;

    @FXML
    private TextField customerSegmentField;

    @FXML
    private CheckBox complianceApprovedCheckBox;

    @FXML
    private TextArea reviewCommentArea;

    @FXML
    private Label statusLabel;

    private OrderValidationService orderValidationService;
    private OrderDecisionExportService orderDecisionExportService;

    public void initialize() {
        statusLabel.setText("Ready for review");
    }

    @FXML
    public void onValidateOrder() {
        if (orderNumberField.getText().isBlank()) {
            statusLabel.setText("RG-ORDER-01: order number is mandatory.");
            return;
        }
        if (!complianceApprovedCheckBox.isSelected()) {
            statusLabel.setText("RG-ORDER-02: compliance approval is required before validation.");
            return;
        }

        orderValidationService.validateOrder(
                orderNumberField.getText(),
                customerSegmentField.getText(),
                reviewCommentArea.getText()
        );
        statusLabel.setText("Order validated");
    }

    @FXML
    public void onExportDecision() {
        orderDecisionExportService.exportDecision(orderNumberField.getText(), reviewCommentArea.getText());
        statusLabel.setText("Decision exported");
    }
}
