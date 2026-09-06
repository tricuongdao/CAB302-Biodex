package com.biodex.controller.heatmap;

import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;

/**
 * Heat map screen with density blobs, filters and a side panel.
 * Static UI only - the report button is wired, the filters are not yet.
 */
public class HeatMapController extends BaseController {

    /** Injected from the fx:include with fx:id="sidebar" in HeatMapView.fxml. */
    @FXML
    private SidebarController sidebarController;

    @FXML
    private void initialize() {
        sidebarController.setActive("heatmap");
    }

    @FXML
    private void onReportSighting() {
        router.go(Route.IDENTIFY_PEST);
    }
}
