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

    /** Landing screen shown at startup. */
    HOME("/com/biodex/fxml/home/HomeView.fxml", "Biodex");

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
