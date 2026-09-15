package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.dao.UserDAO;
import com.biodex.model.User;
import com.biodex.routing.Route;
import com.biodex.util.PasswordHasher;
import com.biodex.util.Validator;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Create account screen. Buttons navigate only - no validation yet.
 */
public class SignupController extends BaseController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    /** Pretends to create the account and opens the heat map. */
    @FXML
    private void onCreateAccount() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        if (!Validator.isValidUsername(username) || !Validator.isValidEmail(email)
                || !Validator.isStrongPassword(password) || !password.equals(confirmPasswordField.getText())) {
            return;
        }
        User user = new User(username, email, PasswordHasher.hash(password));
        new UserDAO().insert(user);
        session.setCurrentUser(user);
        router.go(Route.HEAT_MAP);
    }

    @FXML
    private void onBackToLogin() {
        router.go(Route.LOGIN);
    }
}
