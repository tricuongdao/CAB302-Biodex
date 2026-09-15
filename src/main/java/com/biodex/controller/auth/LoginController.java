package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.dao.UserDAO;
import com.biodex.routing.Route;
import com.biodex.util.PasswordHasher;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Sign in screen. Buttons navigate only - the form accepts any input for now.
 */
public class LoginController extends BaseController {

    @FXML
    private TextField usernameOrEmailField;

    @FXML
    private PasswordField passwordField;

    /** Pretends to sign in and opens the heat map. */
    @FXML
    private void onSignIn() {
        if (usernameOrEmailField == null || passwordField == null) {
            return;
        }
        new UserDAO().findByUsernameOrEmail(usernameOrEmailField.getText().trim())
                .filter(user -> PasswordHasher.verify(passwordField.getText(), user.getPasswordHash()))
                .ifPresent(user -> {
                    session.setCurrentUser(user);
                    router.go(Route.HEAT_MAP);
                });
    }

    @FXML
    private void onCreateAccount() {
        router.go(Route.SIGNUP);
    }

    @FXML
    private void onForgotPassword() {
        router.go(Route.FORGOT_PASSWORD);
    }
}
