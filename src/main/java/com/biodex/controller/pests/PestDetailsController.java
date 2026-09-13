package com.biodex.controller.pests;

import com.biodex.api.ServiceFactory;
import com.biodex.api.SpeciesImageResolver;
import com.biodex.api.SpeciesService;
import com.biodex.api.dto.SpeciesSummary;
import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.dao.SpeciesDAO;
import com.biodex.model.Species;
import com.biodex.routing.Route;
import com.biodex.session.SpeciesSelection;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Species search backed by the ALA species service, with local curated species shown on startup.
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
    private final SpeciesDAO localSpeciesDAO = new SpeciesDAO();
    private final SpeciesImageResolver imageResolver = new SpeciesImageResolver(speciesService);

    private Timeline debounce;
    private Task<List<SpeciesSummary>> activeSearch;

    /** Card photo regions by species guid, so the background image task can paint into them. */
    private final Map<String, Region> cardPhotos = new HashMap<>();

    /** Bumps every time the grid is rebuilt; stale image callbacks check it before painting. */
    private int gridGeneration;

    @FXML
    private void initialize() {
        sidebarController.setActive("pests");
        searchField.textProperty().addListener((observable, old, query) -> scheduleSearch(query));
        loadLocalSpecies(); // Show local species immediately
    }

    /** Loads local curated species on startup so the page isn't empty. */
    private void loadLocalSpecies() {
        Task<List<Species>> task = new Task<>() {
            @Override
            protected List<Species> call() {
                return localSpeciesDAO.findAll();
            }
        };
        task.setOnSucceeded(e -> showLocalSpecies(task.getValue()));
        task.setOnFailed(e -> showEmptyState());
        new Thread(task, "local-species-loader").start();
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
        gridGeneration++;
        String snapshot = query == null ? "" : query.trim();
        if (snapshot.isEmpty()) {
            loadLocalSpecies(); // Return to local species when search cleared
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

    /** Shows local species cards. */
    private void showLocalSpecies(List<Species> species) {
        gridGeneration++;
        cardPhotos.clear();
        cardsPane.getChildren().clear();
        for (Species s : species) {
            cardsPane.getChildren().add(cardForLocal(s));
        }
        countLabel.setText("Showing " + species.size() + " local species - search to find more");
        enrichLocalCardImages(species);
    }

    /** Clears the grid and explains that the screen is search-driven. */
    private void showEmptyState() {
        gridGeneration++;
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

    /** Card for a local Species (from our curated DB). */
    private VBox cardForLocal(Species species) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "clickable");
        card.setPrefWidth(300);
        card.setCursor(Cursor.HAND);

        Region photo = new Region();
        photo.setPrefHeight(90);
        photo.getStyleClass().add("pest-photo");
        if (species.getPhotoPath() != null && !species.getPhotoPath().isBlank()) {
            applyPhoto(photo, species.getPhotoPath());
        }
        if (species.getAlaGuid() != null) {
            cardPhotos.put(species.getAlaGuid(), photo);
        }

        Label name = new Label(species.getCommonName());
        name.getStyleClass().add("section-title");
        name.setWrapText(true);

        Label scientific = new Label(orBlank(species.getScientificName()));
        scientific.getStyleClass().addAll("muted", "sci-name");
        scientific.setWrapText(true);

        card.getChildren().addAll(photo, name, scientific);
        // Wrap local species in a SpeciesSummary for navigation
        card.setOnMouseClicked(event -> openSpecies(toSummary(species)));
        return card;
    }

    private SpeciesSummary toSummary(Species s) {
        return new SpeciesSummary(s.getAlaGuid(), s.getScientificName(), s.getCommonName(), s.getPhotoPath());
    }

    /**
     * Fills the cards that autocomplete returned without a photo. Each missing image resolves
     * through the shared resolver — image-only fetch, in-memory dedup, disk cache underneath —
     * and paints itself the moment it arrives instead of waiting for the slowest card.
     */
    private void enrichCardImages(List<SpeciesSummary> results) {
        int generation = gridGeneration;
        for (SpeciesSummary species : results) {
            if (species.getGuid() == null || species.getImageUrl() != null) {
                continue;
            }
            if (!cardPhotos.containsKey(species.getGuid())) {
                continue;
            }
            String guid = species.getGuid();
            imageResolver.imageForGuid(guid).whenComplete((url, error) ->
                    paintCardImage(generation, guid, url));
        }
    }

    /**
     * Local seeds have no photo and carry {@code fake:} guids ALA cannot resolve, so the real
     * taxon is found once by scientific name (then remembered) and its ALA image painted onto
     * the card as it arrives.
     */
    private void enrichLocalCardImages(List<Species> species) {
        int generation = gridGeneration;
        for (Species entry : species) {
            if (entry.getPhotoPath() != null && !entry.getPhotoPath().isBlank()) {
                continue;
            }
            if (entry.getScientificName() == null || entry.getScientificName().isBlank()) {
                continue;
            }
            if (!cardPhotos.containsKey(entry.getAlaGuid())) {
                continue;
            }
            String key = entry.getAlaGuid();
            imageResolver.imageForScientificName(entry.getScientificName())
                    .whenComplete((url, error) -> paintCardImage(generation, key, url));
        }
    }

    /** Paints one resolved card image; stale grids and failures keep the placeholder. */
    private void paintCardImage(int generation, String key, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        Platform.runLater(() -> {
            if (generation != gridGeneration) {
                return;
            }
            Region photo = cardPhotos.get(key);
            if (photo != null) {
                preloadCardPhoto(key, photo, url);
            }
        });
    }

    /**
     * Preloads the image bytes before swapping the background, so a broken URL never leaves a
     * half-painted card: failures are forgotten (retryable) and the placeholder stays.
     */
    private void preloadCardPhoto(String key, Region photo, String url) {
        Image image = new Image(url, 220, 140, true, true, true);
        Runnable apply = () -> {
            if (!image.isError() && image.getWidth() > 0) {
                applyPhoto(photo, url);
            } else {
                imageResolver.forget(guidForCardKey(key));
            }
        };
        if (image.getProgress() >= 1 || image.isError()) {
            apply.run();
        } else {
            image.progressProperty().addListener((observable, oldProgress, progress) -> {
                if (progress.doubleValue() >= 1 || image.isError()) {
                    apply.run();
                }
            });
            image.errorProperty().addListener((observable, wasError, isError) -> {
                if (isError) {
                    apply.run();
                }
            });
        }
    }

    /** Card keys are real guids for ALA results and {@code fake:} guids for local seeds. */
    private String guidForCardKey(String key) {
        return key;
    }

    /** Paints an image onto a card photo region, replacing the plain placeholder background. */
    private static void applyPhoto(Region photo, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String escaped = url.replace("'", "%27");
        photo.setStyle("-fx-background-size: cover; -fx-background-position: center;"
                + " -fx-background-image: url('" + escaped + "');");
    }

    private void openSpecies(SpeciesSummary species) {
        SpeciesSelection.getInstance().setCurrent(species);
        router.go(Route.PEST_DETAIL);
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
