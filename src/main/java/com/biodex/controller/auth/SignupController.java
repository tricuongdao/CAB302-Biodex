package com.biodex.controller.auth;

import com.biodex.controller.BaseController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;

/**
 * Create account screen. Buttons navigate only - no validation yet.
 */
public class SignupController extends BaseController {

    /** Pretends to create the account and opens the heat map. */
    @FXML
    private void onCreateAccount() {
        router.go(Route.HEAT_MAP);
    }

    @FXML
    private void onBackToLogin() {
        router.go(Route.LOGIN);
    }
}
