package com.biodex.controller.pests;

import com.biodex.api.dto.SpeciesSummary;
import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.dao.SpeciesDAO;
import com.biodex.model.Species;
import com.biodex.routing.Route;
import com.biodex.session.SpeciesSelection;

import javafx.concurrent.Task;
import javafx.fxml.FXML;

import java.util.Optional;

/**
 * Parent controller for the Pest Detail screen.
 * Loads the local curated Species record once, then delegates display to the two child panes:
 * - SpeciesDetailController (left): photo, threat profile, facts, disposal guidance
 * - LocalSightingsController (right): density cards, recent reports
 */
public class PestDetailController extends BaseController {

    @FXML private SidebarController sidebarController;
    @FXML private SpeciesDetailController speciesDetailController;
    @FXML private LocalSightingsController localSightingsController;
    @FXML private javafx.scene.control.Label breadcrumbLabel;

    private final SpeciesDAO speciesDAO = new SpeciesDAO();

    @FXML
        private void initialize() {
            sidebarController.setActive("pests");

            // Ensure species data is seeded before loading
            speciesDAO.seedIfEmpty();

            SpeciesSummary selection = SpeciesSelection.getInstance().getCurrent();
            if (selection == null) {
                showNoSelection();
                return;
            }

            breadcrumbLabel.setText("/ " + selection.displayName());
            loadLocalSpecies(selection);
        }

    /** Loads the local Species record by ALA guid or scientific name. */
    private void loadLocalSpecies(SpeciesSummary selection) {
        Task<Optional<Species>> task = new Task<>() {
            @Override
            protected Optional<Species> call() {
                // Try by ALA guid first
                if (selection.getGuid() != null && !selection.getGuid().isBlank()) {
                    Optional<Species> byGuid = speciesDAO.findByAlaGuid(selection.getGuid());
                    if (byGuid.isPresent()) return byGuid;
                }
                // Fall back to scientific name
                if (selection.getScientificName() != null && !selection.getScientificName().isBlank()) {
                    return speciesDAO.findByScientificName(selection.getScientificName());
                }
                return Optional.empty();
            }
        };
        task.setOnSucceeded(e -> {
            Optional<Species> speciesOpt = task.getValue();
            if (speciesOpt.isPresent()) {
                Species species = speciesOpt.get();
                speciesDetailController.display(species);
                localSightingsController.load(species.getSpeciesId());
            } else {
                showNotInLocalDB(selection);
            }
        });
        task.setOnFailed(e -> showNotInLocalDB(selection));

        new Thread(task, "local-species-loader").start();
    }

    private void showNoSelection() {
        breadcrumbLabel.setText("/ nothing selected");
        speciesDetailController.display(null);
        localSightingsController.load(-1);
    }

    private void showNotInLocalDB(SpeciesSummary selection) {
        breadcrumbLabel.setText("/ " + selection.displayName() + " (not in local DB)");
        // Could show a placeholder in the child panes
    }

    @FXML
    private void onBackToSpeciesSearch() {
        router.go(Route.SPECIES_SEARCH);
    }

}
