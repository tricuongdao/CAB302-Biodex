package com.biodex.controller.profile;

import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;

/**
 * Profile & settings screen. Static UI only - sign out is wired.
 */
public class ProfileController extends BaseController {

    /** Injected from the fx:include with fx:id="sidebar" in ProfileView.fxml. */
    @FXML
    private SidebarController sidebarController;

    @FXML
    private void initialize() {
        sidebarController.setActive("profile");
    }

    @FXML
    private void onSignOut() {
        router.go(Route.LOGIN);
    }
}
