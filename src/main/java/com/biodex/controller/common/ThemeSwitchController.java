package com.biodex.controller.common;

import com.biodex.controller.BaseController;
import com.biodex.dao.SettingsDAO;
import com.biodex.util.ThemeManager;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.ToggleButton;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Animated light/dark switch shared by the application screens.
 */
public class ThemeSwitchController extends BaseController {

    private ParallelTransition toggleAnimation;

    @FXML
    private ToggleButton themeToggle;

    @FXML
    private Circle toggleKnob;

    @FXML
    private void initialize() {
        syncWithCurrentTheme();
        Platform.runLater(this::syncWithCurrentTheme);
    }

    @FXML
    private void onThemeToggle() {
        boolean isDark = themeToggle.isSelected();
        String theme = isDark ? ThemeManager.DARK : ThemeManager.LIGHT;
        animateToggle(isDark);
        ThemeManager.setCurrentTheme(theme);
        ThemeManager.apply(themeToggle.getScene(), theme);
        if (currentUser() != null) {
            new SettingsDAO().updateSetting(currentUser().getUserId(), SettingsDAO.SettingColumn.THEME,
                    theme);
        }
    }

    public void syncWithCurrentTheme() {
        if (themeToggle == null || toggleKnob == null) {
            return;
        }

        boolean isDark = ThemeManager.DARK.equals(ThemeManager.getCurrentTheme());
        themeToggle.setSelected(isDark);
        toggleKnob.setTranslateX(isDark ? 28 : 0);
        themeToggle.setAccessibleText(isDark ? "Switch to light theme" : "Switch to dark theme");
    }

    private void animateToggle(boolean isDark) {
        if (toggleAnimation != null) {
            toggleAnimation.stop();
        }

        TranslateTransition slide = new TranslateTransition(Duration.millis(220), toggleKnob);
        slide.setToX(isDark ? 28 : 0);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition bounce = new ScaleTransition(Duration.millis(110), toggleKnob);
        bounce.setFromX(1);
        bounce.setFromY(1);
        bounce.setToX(0.82);
        bounce.setToY(0.82);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(2);
        bounce.setInterpolator(Interpolator.EASE_BOTH);

        toggleAnimation = new ParallelTransition(slide, bounce);
        toggleAnimation.play();
    }
}
