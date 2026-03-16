package app.ui.payment;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class PaymentEscalationController {

    @FXML
    private TextField paymentReferenceField;

    @FXML
    private TextField amountField;

    @FXML
    private CheckBox fraudAlertCheckBox;

    @FXML
    private TextArea escalationReasonArea;

    @FXML
    private Label feedbackLabel;

    private PaymentEscalationService paymentEscalationService;
    private NotificationService notificationService;

    @FXML
    public void onEscalatePayment() {
        if (paymentReferenceField.getText().isBlank()) {
            feedbackLabel.setText("RG-PAY-01: payment reference is required.");
            return;
        }
        if (fraudAlertCheckBox.isSelected() && escalationReasonArea.getText().isBlank()) {
            feedbackLabel.setText("RG-PAY-02: fraud escalations require a reason.");
            return;
        }

        paymentEscalationService.escalate(
                paymentReferenceField.getText(),
                amountField.getText(),
                escalationReasonArea.getText()
        );
        notificationService.notifyFraudDesk(paymentReferenceField.getText());
        feedbackLabel.setText("Escalation sent");
    }
}
