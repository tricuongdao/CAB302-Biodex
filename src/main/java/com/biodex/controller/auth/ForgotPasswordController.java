package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.dao.PasswordResetDAO;
import com.biodex.dao.UserDAO;
import com.biodex.model.User;
import com.biodex.routing.Route;
import com.biodex.session.PasswordResetSession;
import com.biodex.util.Validator;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.time.Duration;

/**
 * Handles password recovery requests.
 *
 * The fixed recovery code is used only for the assessment prototype.
 */
public class ForgotPasswordController extends BaseController {

    private static final String RECOVERY_CODE = "111111";

    @FXML
    private TextField emailField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button sendCodeButton;

    @FXML
    private ProgressIndicator progressIndicator;

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetDAO resetDAO = new PasswordResetDAO();
    private final PasswordResetSession resetSession =
            PasswordResetSession.getInstance();

    @FXML
    private void onSendCode() {
        String email = emailField.getText().trim();

        hideError();

        if (!Validator.isValidEmail(email)) {
            showError("Enter a valid email address.");
            return;
        }

        resetSession.clear();
        setBusy(true);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                User user = userDAO.findByEmail(email).orElse(null);

                if (user == null) {
                    return null;
                }

                // stores a hash of the fixed prototype code
                resetDAO.createResetCode(
                        user.getUserId(),
                        RECOVERY_CODE,
                        Duration.ofMinutes(15));

                return user.getUserId();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            Integer userId = task.getValue();

            if (userId == null) {
                showError("No account was found with that email address.");
                return;
            }

            resetSession.setEmail(email);
            resetSession.setUserId(userId);
            resetSession.setCodeVerified(false);

            router.go(Route.VERIFY_CODE);
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError("Unable to start password recovery. Please try again.");
        });

        Thread thread = new Thread(task, "password-reset-code-request");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onBackToLogin() {
        resetSession.clear();
        router.go(Route.LOGIN);
    }

    private void setBusy(boolean busy) {
        emailField.setDisable(busy);
        sendCodeButton.setDisable(busy);
        progressIndicator.setManaged(busy);
        progressIndicator.setVisible(busy);
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