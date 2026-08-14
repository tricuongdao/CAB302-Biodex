package com.biodex;

import com.biodex.db.DatabaseConnection;
import com.biodex.db.SchemaInitialiser;
import com.biodex.routing.Route;
import com.biodex.routing.SceneRouter;
import javafx.application.Application;
import javafx.stage.Stage;

/** Application entry point: prepares the database, then opens the home screen. */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        SchemaInitialiser.initialise();

        SceneRouter router = SceneRouter.getInstance();
        router.init(stage);
        router.go(Route.HOME);
        stage.show();
    }

    @Override
    public void stop() {
        DatabaseConnection.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
