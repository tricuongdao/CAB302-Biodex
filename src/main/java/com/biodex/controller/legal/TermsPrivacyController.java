package com.biodex.controller.legal;

import com.biodex.controller.BaseController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;

/**
 * Displays the terms privacy and data information shown during signup
 */
public class TermsPrivacyController extends BaseController {

    @FXML
    private void onBackToSignup() {
        router.go(Route.SIGNUP);
    }
}