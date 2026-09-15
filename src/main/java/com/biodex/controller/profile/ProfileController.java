package com.biodex.controller.profile;

import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.dao.DataAccessException;
import com.biodex.dao.SettingsDAO;
import com.biodex.dao.UserDAO;
import com.biodex.routing.Route;
import com.biodex.util.ThemeManager;
import com.biodex.util.Validator;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Profile and account settings screen.
 */
public class ProfileController extends BaseController {

    /** Injected from the fx:include with fx:id="sidebar" in ProfileView.fxml. */
    @FXML
    private SidebarController sidebarController;

    @FXML
    private Label profileEmail;

    @FXML
    private Label profileEmailDetail;

    @FXML
    private Label profileVerifiedEmail;

    @FXML
    private Label profileUsername;

    @FXML
    private Label profileInitials;

    @FXML
    private TextField emailField;

    @FXML
    private Label emailStatus;

    @FXML
    private Button lightThemeButton;

    @FXML
    private Button darkThemeButton;

    @FXML
    private Button autoThemeButton;

    @FXML
    private void initialize() {
        sidebarController.setActive("profile");
        if (currentUser() != null) {
            profileUsername.setText(currentUser().getUsername());
            profileEmail.setText(currentUser().getEmail());
            profileEmailDetail.setText(currentUser().getEmail());
            profileVerifiedEmail.setText(currentUser().getEmail());
            emailField.setText(currentUser().getEmail());
            profileInitials.setText(initials(currentUser().getUsername()));
            ThemeManager.setCurrentTheme(new SettingsDAO().getSettingsForUser(
                    currentUser().getUserId()).getTheme());
        }
        updateThemeButtons();
    }

    @FXML
    private void onSaveEmail() {
        if (currentUser() == null) {
            return;
        }

        String email = emailField.getText().trim();
        if (!Validator.isValidEmail(email)) {
            showEmailStatus("Please enter a valid email address.", true);
            return;
        }

        if (!email.equals(currentUser().getEmail())
                && new UserDAO().findByEmail(email).isPresent()) {
            showEmailStatus("That email address is already in use.", true);
            return;
        }

        try {
            if (new UserDAO().updateEmail(currentUser().getUserId(), email)) {
                currentUser().setEmail(email);
                profileEmail.setText(email);
                profileEmailDetail.setText(email);
                profileVerifiedEmail.setText(email);
                showEmailStatus("Email updated.", false);
            } else {
                showEmailStatus("We could not update your email.", true);
            }
        } catch (DataAccessException exception) {
            showEmailStatus("We could not update your email.", true);
        }
    }

    private void showEmailStatus(String message, boolean error) {
        emailStatus.setText(message);
        emailStatus.getStyleClass().removeAll("error-label", "success-label");
        emailStatus.getStyleClass().add(error ? "error-label" : "success-label");
    }

    @FXML
    private void onLightTheme() {
        applyTheme(ThemeManager.LIGHT);
    }

    @FXML
    private void onDarkTheme() {
        applyTheme(ThemeManager.DARK);
    }

    @FXML
    private void onAutoTheme() {
        applyTheme(ThemeManager.LIGHT);
    }

    private void applyTheme(String theme) {
        ThemeManager.setCurrentTheme(theme);
        ThemeManager.apply(profileUsername.getScene(), theme);
        if (currentUser() != null) {
            new SettingsDAO().updateSetting(currentUser().getUserId(), SettingsDAO.SettingColumn.THEME,
                    theme);
        }
        updateThemeButtons();
    }

    private void updateThemeButtons() {
        lightThemeButton.getStyleClass().remove("chip-selected");
        darkThemeButton.getStyleClass().remove("chip-selected");
        autoThemeButton.getStyleClass().remove("chip-selected");
        (ThemeManager.DARK.equals(ThemeManager.getCurrentTheme()) ? darkThemeButton : lightThemeButton)
                .getStyleClass().add("chip-selected");
    }

    private static String initials(String username) {
        if (username == null || username.isBlank()) {
            return "?";
        }
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    @FXML
    private void onSignOut() {
        session.clear();
        router.go(Route.LOGIN);
    }

    @FXML
    private void onChangePassword() {
        router.go(Route.FORGOT_PASSWORD);
    }
}
