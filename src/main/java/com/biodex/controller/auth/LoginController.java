package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.routing.Route;
import com.biodex.service.AuthService;
import com.biodex.service.LoginResult;
import com.biodex.util.Entrance;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController extends BaseController {

    @FXML private TextField usernameOrEmailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button signInButton;
    @FXML private Button googleSignInButton;
    @FXML private VBox formBox;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        Entrance.play(formBox);
    }

    @FXML
    private void onSignIn() {
        String usernameOrEmail = textOf(usernameOrEmailField);
        String password = passwordField.getText();

        if (usernameOrEmail.isEmpty() || password == null || password.isEmpty()) {
            showError("Enter your username or email and password.");
            return;
        }

        setBusy(true);

        Task<LoginResult> task = new Task<>() {
            @Override
            protected LoginResult call() {
                return authService.login(usernameOrEmail, password);
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

        new Thread(task, "login-task").start();
    }

    @FXML
    private void onCreateAccount() {
        router.go(Route.SIGNUP);
    }

    @FXML
    private void onForgotPassword() {
        router.go(Route.FORGOT_PASSWORD);
    }

    @FXML
    private void onGoogleSignIn() {
        // Button for cosmetics
    }

    private void handleResult(LoginResult result) {
        switch (result.getStatus()) {
            case SUCCESS -> {
                session.setCurrentUser(result.getUser());
                router.go(Route.HEAT_MAP);
            }
            case INVALID_CREDENTIALS -> showError("Incorrect email or password.");
        }
    }

    private void setBusy(boolean busy) {
        usernameOrEmailField.setDisable(busy);
        passwordField.setDisable(busy);
        signInButton.setDisable(busy);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private static String textOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
