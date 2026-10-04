package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.routing.Route;
import com.biodex.service.AuthService;
import com.biodex.service.LoginResult;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class LoginController extends BaseController {

    @FXML private TextField usernameOrEmailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button signInButton;
    @FXML private Button googleSignInButton;
    @FXML private VBox brandPanel;
    @FXML private VBox formBox;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        playEntrance();
    }

    /**
     * A short fade-up when the screen opens: occasional screens can afford a little motion while
     * everyday actions stay instant. Strong ease-out curve, 60ms between the two columns.
     */
    private void playEntrance() {
        if (brandPanel == null || formBox == null) {
            return;
        }
        Interpolator easeOut = Interpolator.SPLINE(0.23, 1, 0.32, 1);
        Node[] columns = {brandPanel, formBox};
        for (int i = 0; i < columns.length; i++) {
            Node column = columns[i];
            column.setOpacity(0);
            column.setTranslateY(14);

            FadeTransition fade = new FadeTransition(Duration.millis(350), column);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(i * 60L));
            fade.setInterpolator(easeOut);

            TranslateTransition rise = new TranslateTransition(Duration.millis(350), column);
            rise.setFromY(14);
            rise.setToY(0);
            rise.setDelay(Duration.millis(i * 60L));
            rise.setInterpolator(easeOut);

            fade.play();
            rise.play();
        }
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

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private static String textOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
