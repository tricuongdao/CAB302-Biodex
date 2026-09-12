package com.biodex.controller.identify;

import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;

/**
 * Identify a pest screen: photo drop zone, possible matches and a sighting form.
 * Static UI only - upload and matching are stubs for a later sprint.
 */
public class IdentifyPestController extends BaseController {

    /** Injected from the fx:include with fx:id="sidebar" in IdentifyPestView.fxml. */
    @FXML
    private SidebarController sidebarController;

    @FXML
    private void initialize() {
        sidebarController.setActive("identify");
    }

    @FXML
    private void onBrowseFiles() {
        // Feature sprint: open a FileChooser and show previews.
    }

    @FXML
    private void onBackToHeatMap() {
        router.go(Route.HEAT_MAP);
    }

    @FXML
    private void onSubmitReport() {
        router.go(Route.HEAT_MAP);
    }
}
