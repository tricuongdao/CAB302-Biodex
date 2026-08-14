package com.biodex.routing;

import com.biodex.util.ThemeManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Swaps the window between screens.
 *
 * <p>Registry driven: the router knows nothing about individual pages, only how to load whatever
 * {@link Route} it is handed. {@code SceneRouter.getInstance().go(Route.LOGIN)} loads that route's
 * FXML, applies the current theme and sets the window title.
 */
public final class SceneRouter {

    private static final int DEFAULT_WIDTH = 900;
    private static final int DEFAULT_HEIGHT = 600;

    private static SceneRouter instance;

    private Stage stage;
    private Route currentRoute;

    private SceneRouter() {
    }

    /** Returns the singleton instance. */
    public static synchronized SceneRouter getInstance() {
        if (instance == null) {
            instance = new SceneRouter();
        }
        return instance;
    }

    /** Hands the router the primary stage. Called once, by {@code Main}. */
    public void init(Stage stage) {
        this.stage = stage;
    }

    /** The route currently on screen, or null before the first navigation. */
    public Route getCurrentRoute() {
        return currentRoute;
    }

    /** Loads a route into the window. */
    public void go(Route route) {
        if (stage == null) {
            throw new IllegalStateException("SceneRouter.init(Stage) must be called before navigating");
        }

        URL fxml = SceneRouter.class.getResource(route.getFxmlPath());
        if (fxml == null) {
            throw new IllegalStateException(
                    "FXML not found for route " + route + ": " + route.getFxmlPath());
        }

        try {
            Parent root = new FXMLLoader(fxml).load();
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            ThemeManager.apply(scene, ThemeManager.getCurrentTheme());
            stage.setTitle(route.getTitle());
            currentRoute = route;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load route " + route, e);
        }
    }
}
