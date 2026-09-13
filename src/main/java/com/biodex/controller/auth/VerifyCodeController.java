package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.dao.PasswordResetDAO;
import com.biodex.routing.Route;
import com.biodex.session.PasswordResetSession;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Handles verification of the six-digit password reset code.
 */
public class VerifyCodeController extends BaseController {

    @FXML
    private Label emailLabel;

    @FXML
    private TextField codeField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button verifyButton;

    private final PasswordResetDAO resetDAO = new PasswordResetDAO();
    private final PasswordResetSession resetSession =
            PasswordResetSession.getInstance();

    @FXML
    private void initialize() {
        String email = resetSession.getEmail();

        if (email == null) {
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
            showError("Enter the six-digit code from your email.");
            return;
        }

        setBusy(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return resetDAO.verifyCode(resetSession.getEmail(), code);
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            if (Boolean.TRUE.equals(task.getValue())) {
                resetSession.setCodeVerified(true);
                router.go(Route.RESET_PASSWORD);
            } else {
                codeField.clear();
                showError("Incorrect or expired code. Please request a new code.");
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError("Unable to verify the code. Please try again.");
        });

        Thread thread = new Thread(task, "password-reset-code-verification");
        thread.setDaemon(true);
        thread.start();
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

    private void setBusy(boolean busy) {
        codeField.setDisable(busy);
        verifyButton.setDisable(busy);
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