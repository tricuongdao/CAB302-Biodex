package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.dao.UserDAO;
import com.biodex.routing.Route;
import com.biodex.session.PasswordResetSession;
import com.biodex.util.PasswordHasher;
import com.biodex.util.Validator;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;

/**
 * Handles the final password reset stage.
 */
public class ResetPasswordController extends BaseController {

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button resetButton;

    @FXML
    private ProgressIndicator progressIndicator;

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetSession resetSession =
            PasswordResetSession.getInstance();

    @FXML
    private void initialize() {
        if (!hasVerifiedReset()) {
            showError("Verify a password reset code before changing your password.");
            setFormDisabled(true);
        }
    }

    @FXML
    private void onResetPassword() {
        if (!hasVerifiedReset()) {
            showError("Your password reset session is no longer valid.");
            setFormDisabled(true);
            return;
        }

        String newPassword = newPasswordField.getText();
        String confirmedPassword = confirmPasswordField.getText();

        hideError();

        if (!Validator.isStrongPassword(newPassword)) {
            showError(
                    "Password must have at least 8 characters, including a letter and number.");
            return;
        }

        if (!newPassword.equals(confirmedPassword)) {
            showError("Passwords do not match.");
            return;
        }

        int userId = resetSession.getUserId();

        setBusy(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                String passwordHash = PasswordHasher.hash(newPassword);

                return userDAO.updatePasswordHash(userId, passwordHash);
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            if (Boolean.TRUE.equals(task.getValue())) {
                resetSession.clear();
                router.go(Route.LOGIN);
            } else {
                showError("Unable to find the account for this password reset.");
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError("Unable to update the password. Please try again.");
        });

        Thread thread = new Thread(task, "password-reset-update");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onCancel() {
        resetSession.clear();
        router.go(Route.LOGIN);
    }

    private boolean hasVerifiedReset() {
        return resetSession.getEmail() != null
                && resetSession.getUserId() > 0
                && resetSession.isCodeVerified();
    }

    private void setBusy(boolean busy) {
        newPasswordField.setDisable(busy);
        confirmPasswordField.setDisable(busy);
        resetButton.setDisable(busy);
        progressIndicator.setManaged(busy);
        progressIndicator.setVisible(busy);
    }

    private void setFormDisabled(boolean disabled) {
        newPasswordField.setDisable(disabled);
        confirmPasswordField.setDisable(disabled);
        resetButton.setDisable(disabled);
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
}