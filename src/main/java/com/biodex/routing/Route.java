package com.biodex.routing;

/**
 * The registry of every screen in Biodex.
 *
 * <p>This enum is the only shared file a page author edits. Adding a page means adding one entry
 * here pointing at your FXML and window title; {@link SceneRouter#go(Route)} does the rest:
 *
 * <pre>{@code
 * LOGIN("/com/biodex/fxml/login/LoginView.fxml", "Biodex - Sign in"),
 * }</pre>
 *
 * <p>Keep one entry per line so two people adding pages on different branches do not conflict.
 */
public enum Route {

    LOGIN("/com/biodex/fxml/auth/LoginView.fxml", "Biodex - Sign in"),
    SIGNUP("/com/biodex/fxml/auth/SignupView.fxml", "Biodex - Create account"),
    FORGOT_PASSWORD("/com/biodex/fxml/auth/ForgotPasswordView.fxml", "Biodex - Account recovery"),
    VERIFY_CODE("/com/biodex/fxml/auth/VerifyCodeView.fxml", "Biodex - Verify code"),
    RESET_PASSWORD("/com/biodex/fxml/auth/ResetPasswordView.fxml", "Biodex - Reset password"),
    HEAT_MAP("/com/biodex/fxml/heatmap/HeatMapView.fxml", "Biodex - Heat map"),
    IDENTIFY_PEST("/com/biodex/fxml/identify/IdentifyPestView.fxml", "Biodex - Identify a pest"),
    SPECIES_SEARCH("/com/biodex/fxml/pests/SpeciesSearchView.fxml", "Biodex - Species search"),
    PEST_DETAIL("/com/biodex/fxml/pests/PestDetailView.fxml", "Biodex - Species detail"),
    PROFILE("/com/biodex/fxml/profile/ProfileView.fxml", "Biodex - Profile & settings");

    private final String fxmlPath;
    private final String title;

    Route(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    /** Classpath location of the FXML for this screen. */
    public String getFxmlPath() {
        return fxmlPath;
    }

    /** Window title shown while this screen is open. */
    public String getTitle() {
        return title;
    }
}
