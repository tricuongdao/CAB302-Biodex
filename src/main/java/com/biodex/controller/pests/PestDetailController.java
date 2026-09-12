package com.biodex.controller.pests;

import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;

/**
 * Single pest page (currently the cane toad sample).
 * Static UI only - the report button is wired, the rest is placeholder content.
 */
public class PestDetailController extends BaseController {

    /** Injected from the fx:include with fx:id="sidebar" in PestDetailView.fxml. */
    @FXML
    private SidebarController sidebarController;

    @FXML
    private void initialize() {
        sidebarController.setActive("pests");
    }

    @FXML
    private void onBackToHeatMap() {
        router.go(Route.HEAT_MAP);
    }

    @FXML
    private void onReportSighting() {
        router.go(Route.IDENTIFY_PEST);
    }
}
