package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;

/**
 * Sign in screen. Buttons navigate only - the form accepts any input for now.
 */
public class LoginController extends BaseController {

    /** Pretends to sign in and opens the heat map. */
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
}
