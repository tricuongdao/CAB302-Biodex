package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.routing.Route;
import com.biodex.service.AuthService;
import com.biodex.service.SignupResult;
import com.biodex.util.Validator;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignupController extends BaseController {

    @FXML private TextField displayNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button createAccountButton;

    private final AuthService authService = new AuthService();

    /** Validates the form, creates the account through AuthService, then opens the sign-in screen. */
    @FXML
    private void onCreateAccount() {
        String username = textOf(usernameField);
        String email = textOf(emailField);
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password == null || password.isEmpty()) {
            showError("Please fill in every field.");
            return;
        }
        if (!Validator.isValidUsername(username)) {
            showError("Your username should be 3-20 letters, numbers or underscores.");
            return;
        }
        if (!Validator.isValidEmail(email)) {
            showError("Please enter a valid email address.");
            return;
        }
        if (!Validator.isStrongPassword(password)) {
            showError("Password must be at least " + Validator.MINIMUM_PASSWORD_LENGTH + " characters, with a letter and a number.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        setBusy(true);

        Task<SignupResult> task = new Task<>() {
            @Override
            protected SignupResult call() {
                return authService.signup(username, email, password);
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            handleResult(task.getValue());
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError("Couldn't reach the database. Please try again.");
        });

        new Thread(task, "signup-task").start();
    }

    @FXML
    private void onBackToLogin() {
        router.go(Route.LOGIN);
    }

    private void handleResult(SignupResult result) {
        switch (result.getStatus()) {
            case SUCCESS -> router.go(Route.LOGIN);
            case USERNAME_TAKEN -> showError("That username is already taken.");
            case EMAIL_TAKEN -> showError("An account with that email already exists.");
            case INVALID_USERNAME -> showError("Username must be 3-20 letters, numbers or underscores.");
            case INVALID_EMAIL -> showError("Enter a valid email address.");
            case WEAK_PASSWORD -> showError("Password must be at least "
                    + Validator.MINIMUM_PASSWORD_LENGTH + " characters, with a letter and a number.");
        }
    }

    private void setBusy(boolean busy) {
        displayNameField.setDisable(busy);
        usernameField.setDisable(busy);
        emailField.setDisable(busy);
        passwordField.setDisable(busy);
        confirmPasswordField.setDisable(busy);
        createAccountButton.setDisable(busy);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private static String textOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
