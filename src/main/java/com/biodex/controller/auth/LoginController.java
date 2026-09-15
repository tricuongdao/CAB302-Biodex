package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.dao.UserDAO;
import com.biodex.routing.Route;
import com.biodex.util.PasswordHasher;
import com.biodex.service.AuthService;
import com.biodex.service.LoginResult;

import javafx.concurrent.Task;
import javafx.fxml.FXML;

public class LoginController extends BaseController {

    @FXML
    private TextField usernameOrEmailField;

    @FXML
    private PasswordField passwordField;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button signInButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void onSignIn() {
        router.go(Route.HEAT_MAP);
    }

    @FXML
    private void onCreateAccount() {
        router.go(Route.SIGNUP);
    }

    @FXML
    private void onForgotPassword() {
        router.go(Route.FORGOT_PASSWORD);
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
        emailField.setDisable(busy);
        passwordField.setDisable(busy);
        signInButton.setDisable(busy);
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