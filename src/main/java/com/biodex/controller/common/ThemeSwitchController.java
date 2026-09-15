package com.biodex.controller.common;

import com.biodex.controller.BaseController;
import com.biodex.dao.SettingsDAO;
import com.biodex.util.ThemeManager;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;

/**
 * Light/Dark/Auto switcher included in the top bar of every screen.
 * Applies the chosen stylesheet to whatever scene it is currently part of.
 */
public class ThemeSwitchController extends BaseController {

    @FXML
    private Hyperlink lightLink;

    @FXML
    private void onLight() {
        apply(ThemeManager.LIGHT);
    }

    @FXML
    private void onDark() {
        apply(ThemeManager.DARK);
    }

    private void apply(String theme) {
        ThemeManager.setCurrentTheme(theme);
        ThemeManager.apply(lightLink.getScene(), theme);
        if (currentUser() != null) {
            new SettingsDAO().updateSetting(currentUser().getUserId(), SettingsDAO.SettingColumn.THEME,
                    theme);
        }
    }
}
