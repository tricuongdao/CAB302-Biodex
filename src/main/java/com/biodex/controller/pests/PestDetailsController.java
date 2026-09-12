package com.biodex.controller.pests;

import com.biodex.api.ServiceFactory;
import com.biodex.api.SpeciesService;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;
import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.routing.Route;
import com.biodex.session.SpeciesSelection;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Species search backed by the ALA species service.
 *
 * <p>The search field fires {@link SpeciesService#autocomplete} on a background task, debounced by
 * 300 ms so typing does not spam the Atlas. The screen is entirely search-driven — until the user
 * types something, the grid shows a hint rather than any fixed list of species. Clicking a card
 * records the species in {@link SpeciesSelection} and opens the detail screen.
 */
public class PestDetailsController extends BaseController {

    /** Quiet period after the last keystroke before a search runs. */
    private static final Duration SEARCH_DEBOUNCE = Duration.millis(300);

    /** How many suggestions one search asks the service for. */
    private static final int RESULT_LIMIT = 12;

    /** Injected from the fx:include with fx:id="sidebar" in PestDetailsView.fxml. */
    @FXML
    private SidebarController sidebarController;
    @FXML
    private TextField searchField;
    @FXML
    private TilePane cardsPane;
    @FXML
    private Label countLabel;

    private final SpeciesService speciesService = ServiceFactory.speciesService();

    private Timeline debounce;
    private Task<List<SpeciesSummary>> activeSearch;

    /** Card photo regions by species guid, so the background image task can paint into them. */
    private final Map<String, Region> cardPhotos = new HashMap<>();
    private Task<List<CardImage>> activeImages;

    @FXML
    private void initialize() {
        sidebarController.setActive("pests");
        searchField.textProperty().addListener((observable, old, query) -> scheduleSearch(query));
        showEmptyState();
    }

    /** Waits for the user to stop typing, then runs the search off the FX thread. */
    private void scheduleSearch(String query) {
        if (debounce != null) {
            debounce.stop();
        }
        debounce = new Timeline(new KeyFrame(SEARCH_DEBOUNCE, event -> runSearch(query)));
        debounce.setCycleCount(1);
        debounce.play();
    }

    private void runSearch(String query) {
        if (activeSearch != null) {
            activeSearch.cancel();
        }
        if (activeImages != null) {
            activeImages.cancel();
        }
        String snapshot = query == null ? "" : query.trim();
        if (snapshot.isEmpty()) {
            showEmptyState();
            return;
        }
        Task<List<SpeciesSummary>> task = new Task<>() {
            @Override
            protected List<SpeciesSummary> call() {
                return speciesService.autocomplete(snapshot, RESULT_LIMIT);
            }
        };
        activeSearch = task;
        task.setOnSucceeded(event -> {
            if (activeSearch == task) {
                showResults(task.getValue(), snapshot);
            }
        });
        task.setOnFailed(event -> {
            if (activeSearch == task) {
                showResults(List.of(), snapshot);
            }
        });

        Thread loader = new Thread(task, "species-search-loader");
        loader.setDaemon(true);
        loader.start();
    }

    /** Clears the grid and explains that the screen is search-driven. */
    private void showEmptyState() {
        if (activeImages != null) {
            activeImages.cancel();
        }
        cardPhotos.clear();
        cardsPane.getChildren().clear();
        cardsPane.getChildren().add(hintCard());
        countLabel.setText("Search the Atlas of Living Australia above.");
    }

    private VBox hintCard() {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPrefWidth(300);
        Label title = new Label("Find a species");
        title.getStyleClass().add("section-title");
        Label body = new Label(
                "Type a common or scientific name in the search field above and matching species"
                        + " from the Atlas of Living Australia will appear here.");
        body.getStyleClass().add("muted");
        body.setWrapText(true);
        card.getChildren().addAll(title, body);
        return card;
    }

    private void showResults(List<SpeciesSummary> results, String query) {
        List<SpeciesSummary> safe = results == null ? List.of() : results;
        cardPhotos.clear();
        cardsPane.getChildren().clear();
        for (SpeciesSummary species : safe) {
            cardsPane.getChildren().add(cardFor(species));
        }
        if (safe.isEmpty()) {
            countLabel.setText("No species match \"" + query + "\" - try a different name.");
        } else {
            countLabel.setText("Showing " + safe.size() + " results for \"" + query + "\"");
        }
        enrichCardImages(safe);
    }

    private VBox cardFor(SpeciesSummary species) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "clickable");
        card.setPrefWidth(300);
        card.setCursor(Cursor.HAND);

        Region photo = new Region();
        photo.setPrefHeight(90);
        photo.getStyleClass().add("pest-photo");
        if (species.getImageUrl() != null) {
            applyPhoto(photo, species.getImageUrl());
        }
        if (species.getGuid() != null) {
            cardPhotos.put(species.getGuid(), photo);
        }

        Label name = new Label(displayName(species));
        name.getStyleClass().add("section-title");
        name.setWrapText(true);

        Label scientific = new Label(orBlank(species.getScientificName()));
        scientific.getStyleClass().addAll("muted", "sci-name");
        scientific.setWrapText(true);

        card.getChildren().addAll(photo, name, scientific);
        card.setOnMouseClicked(event -> openSpecies(species));
        return card;
    }

    /**
     * Fills the cards that autocomplete returned without a photo. The Atlas's autocomplete results
     * often omit an image URL; each such species' profile (which carries the enriched ALA photo) is
     * looked up through the cached service, so a plain placeholder card quietly gains its picture —
     * and repeat visits cost nothing.
     */
    private void enrichCardImages(List<SpeciesSummary> results) {
        List<String> needingImages = results.stream()
                .filter(species -> species.getGuid() != null && species.getImageUrl() == null)
                .map(SpeciesSummary::getGuid)
                .filter(cardPhotos::containsKey)
                .toList();
        if (needingImages.isEmpty()) {
            return;
        }
        if (activeImages != null) {
            activeImages.cancel();
        }
        Task<List<CardImage>> task = new Task<>() {
            @Override
            protected List<CardImage> call() {
                List<CardImage> resolved = new ArrayList<>();
                for (String guid : needingImages) {
                    SpeciesProfile profile = speciesService.profile(guid);
                    if (profile != null && profile.getImageUrl() != null) {
                        resolved.add(new CardImage(guid, profile.getImageUrl()));
                    }
                }
                return resolved;
            }
        };
        activeImages = task;
        task.setOnSucceeded(event -> {
            if (activeImages != task) {
                return;
            }
            for (CardImage image : task.getValue()) {
                Region photo = cardPhotos.get(image.guid());
                if (photo != null) {
                    applyPhoto(photo, image.url());
                }
            }
        });
        Thread loader = new Thread(task, "species-card-images");
        loader.setDaemon(true);
        loader.start();
    }

    /** Paints an image onto a card photo region, replacing the plain placeholder background. */
    private static void applyPhoto(Region photo, String url) {
        photo.setStyle("-fx-background-size: cover; -fx-background-position: center;"
                + " -fx-background-image: url('" + url + "');");
    }

    private void openSpecies(SpeciesSummary species) {
        SpeciesSelection.getInstance().setCurrent(species);
        router.go(Route.PEST_DETAIL);
    }

    /** An image resolved in the background for one card, keyed by species guid. */
    private record CardImage(String guid, String url) {
    }

    private static String displayName(SpeciesSummary species) {
        if (species.getCommonName() != null && !species.getCommonName().isBlank()) {
            return species.getCommonName();
        }
        return species.getScientificName() != null ? species.getScientificName() : "Unknown species";
    }

    private static String orBlank(String value) {
        return value != null ? value : "";
    }
}
