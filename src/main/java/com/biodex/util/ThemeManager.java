package com.biodex.util;

import javafx.scene.Scene;

import java.net.URL;

/**
 * Applies a stylesheet to a scene.
 *
 * <p>Theme names match the values stored in {@code user_settings.theme}. Both stylesheets define
 * the same class names, so a page is styled by the theme without knowing which one is active.
 */
public final class ThemeManager {

    /** Theme name for the light stylesheet, and the database default. */
    public static final String LIGHT = "light";

    /** Theme name for the dark stylesheet. */
    public static final String DARK = "dark";

    private static String currentTheme = LIGHT;

    private ThemeManager() {
    }

    /** The theme new scenes are given. */
    public static String getCurrentTheme() {
        return currentTheme;
    }

    /** Records the theme new scenes should be given. Unknown names fall back to light. */
    public static void setCurrentTheme(String theme) {
        currentTheme = DARK.equalsIgnoreCase(theme) ? DARK : LIGHT;
    }

    /**
     * Replaces the scene's stylesheets with the named theme. Unknown names fall back to light.
     */
    public static void apply(Scene scene, String theme) {
        if (scene == null) {
            return;
        }
        String resolved = DARK.equalsIgnoreCase(theme) ? DARK : LIGHT;
        URL stylesheet = ThemeManager.class.getResource("/com/biodex/css/" + resolved + "-theme.css");
        scene.getStylesheets().clear();
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }
}
