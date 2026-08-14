package com.biodex.controller;

import com.biodex.routing.Route;
import com.biodex.routing.SceneRouter;
import com.biodex.session.SessionManager;
import com.biodex.model.User;

/**
 * The base every page controller extends.
 *
 * <p>It hands page authors the router and the session already wired up, so no page repeats that
 * plumbing:
 *
 * <pre>{@code
 * public class LoginController extends BaseController {
 *     @FXML
 *     private void onSignUpClicked() {
 *         go(Route.SIGNUP);
 *     }
 * }
 * }</pre>
 *
 * <p>Controllers are built by {@code FXMLLoader}, which needs a public no-argument constructor —
 * so subclasses should not declare constructors that take arguments.
 */
public abstract class BaseController {

    /** Navigates between screens. */
    protected final SceneRouter router = SceneRouter.getInstance();

    /** Who is signed in. */
    protected final SessionManager session = SessionManager.getInstance();

    /** Shorthand for {@code router.go(route)}. */
    protected void go(Route route) {
        router.go(route);
    }

    /** The signed-in user, or null if nobody is signed in. */
    protected User currentUser() {
        return session.getCurrentUser();
    }
}
