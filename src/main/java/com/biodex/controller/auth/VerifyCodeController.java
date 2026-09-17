package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.routing.Route;
import com.biodex.session.PasswordResetSession;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Handles verification of fixed password recovery code.
 */
public class VerifyCodeController extends BaseController {

    private static final String RECOVERY_CODE = "111111";

    @FXML
    private Label emailLabel;

    @FXML
    private TextField codeField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button verifyButton;

    private final PasswordResetSession resetSession =
            PasswordResetSession.getInstance();

    @FXML
    private void initialize() {
        String email = resetSession.getEmail();

        if (email == null || resetSession.getUserId() <= 0) {
            showError("No password reset request is active.");
            codeField.setDisable(true);
            verifyButton.setDisable(true);
            return;
        }

        emailLabel.setText("We sent a code to " + maskEmail(email));
    }

    @FXML
    private void onVerifyCode() {
        String code = codeField.getText().trim();

        hideError();

        if (!code.matches("\\d{6}")) {
            showError("Enter a six-digit verification code.");
            return;
        }

        if (!RECOVERY_CODE.equals(code)) {
            codeField.clear();
            showError("Incorrect verification code.");
            return;
        }

        resetSession.setCodeVerified(true);
        router.go(Route.RESET_PASSWORD);
    }

    @FXML
    private void onRequestNewCode() {
        resetSession.clear();
        router.go(Route.FORGOT_PASSWORD);
    }

    @FXML
    private void onBackToLogin() {
        resetSession.clear();
        router.go(Route.LOGIN);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private String maskEmail(String email) {
        int atPosition = email.indexOf('@');

        if (atPosition <= 0) {
            return email;
        }

        String firstCharacter = email.substring(0, 1);
        String domain = email.substring(atPosition);

        return firstCharacter + "***" + domain;
    }
}