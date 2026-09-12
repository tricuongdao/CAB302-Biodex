package com.biodex.controller.pests;

import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;

/**
 * Pest details screen: filter chips and a grid of species cards.
 * Static UI only - every card opens the same sample detail page for now.
 */
public class PestDetailsController extends BaseController {

    /** Injected from the fx:include with fx:id="sidebar" in PestDetailsView.fxml. */
    @FXML
    private SidebarController sidebarController;

    @FXML
    private void initialize() {
        sidebarController.setActive("pests");
    }

    @FXML
    private void onOpenCaneToad() {
        router.go(Route.PEST_DETAIL);
    }
}
