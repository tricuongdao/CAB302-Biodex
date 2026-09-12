package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;

/**
 * Account recovery screen. Buttons navigate only - no email sending yet.
 */
public class ForgotPasswordController extends BaseController {

    /** Pretends to send the reset link and returns to the sign in screen. */
    @FXML
    private void onSendResetLink() {
        router.go(Route.LOGIN);
    }

    @FXML
    private void onBackToLogin() {
        router.go(Route.LOGIN);
    }
}
