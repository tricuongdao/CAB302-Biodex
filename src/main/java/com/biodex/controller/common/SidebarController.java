package com.biodex.controller.common;

import java.util.List;

import com.biodex.controller.BaseController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller for the shared navigation rail included by every main screen.
 * Screens call {@link #setActive(String)} to highlight their own nav entry.
 */
public class SidebarController extends BaseController {

    @FXML
    private Button heatMapLink;
    @FXML
    private Button identifyLink;
    @FXML
    private Button pestDetailsLink;
    @FXML
    private Button profileLink;

    @FXML
    private void onHeatMap() {
        router.go(Route.HEAT_MAP);
    }

    @FXML
    private void onIdentifyPest() {
        router.go(Route.IDENTIFY_PEST);
    }

    @FXML
    private void onPestDetails() {
        router.go(Route.PEST_DETAILS);
    }

    @FXML
    private void onProfile() {
        router.go(Route.PROFILE);
    }

    /**
     * Highlights the nav entry for the screen currently showing the sidebar.
     *
     * @param key one of "heatmap", "identify", "pests" or "profile"
     */
    public void setActive(String key) {
        Button target = switch (key) {
            case "identify" -> identifyLink;
            case "pests" -> pestDetailsLink;
            case "profile" -> profileLink;
            default -> heatMapLink;
        };
        for (Button link : List.of(heatMapLink, identifyLink, pestDetailsLink, profileLink)) {
            link.getStyleClass().remove("sidebar-link-active");
        }
        target.getStyleClass().add("sidebar-link-active");
    }
}
