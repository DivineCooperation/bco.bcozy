/**
 * ==================================================================
 *
 * This file is part of org.dc.bco.bcozy.
 *
 * org.dc.bco.bcozy is free software: you can redistribute it and modify
 * it under the terms of the GNU General Public License (Version 3)
 * as published by the Free Software Foundation.
 *
 * org.dc.bco.bcozy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with org.dc.bco.bcozy. If not, see <http://www.gnu.org/licenses/>.
 * ==================================================================
 */
package org.dc.bco.bcozy.controller;

import javafx.event.ActionEvent;
import javafx.stage.Stage;
import org.dc.bco.bcozy.view.CenterPane;
import org.dc.bco.bcozy.view.Constants;
import org.dc.bco.bcozy.view.ForegroundPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by hoestreich on 12/2/15.
 */
public class CenterPaneController {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CenterPaneController.class);
    private final CenterPane centerPane;
    private State activeState;
    private boolean isShowing;
    /**
     * Enum to control the display state.
     */
    public enum State { SETTINGS, TEMPERATURE, MOVEMENT }
    /**
     * Constructor for the CenterPaneController.
     * @param foregroundPane instance of the foregroundPane which has all elements as its children.
     */
    public CenterPaneController(final ForegroundPane foregroundPane) {
        isShowing = false;
        activeState = State.SETTINGS;
        centerPane = foregroundPane.getCenterPane();
        centerPane.getFullscreen().setOnAction(event -> setMaximizeAction());
        centerPane.getPopUpParent().setOnAction(event -> setShowHidePopOver());
        centerPane.getPopUpChildBottom().setOnAction(event -> setChooseView(event));
        centerPane.getPopUpChildTop().setOnAction(event -> setChooseView(event));

        ((Stage) centerPane.getScene().getWindow()).fullScreenProperty().
                addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                centerPane.getFullscreen().changeIcon(Constants.FULLSCREEN_ICON);
            }
        });
    }

    private void setMaximizeAction() {
        final Stage stage = (Stage) centerPane.getScene().getWindow();
        if (stage.isFullScreen()) {
            centerPane.getFullscreen().changeIcon(Constants.FULLSCREEN_ICON);
            stage.setFullScreen(false);
        } else {
            centerPane.getFullscreen().changeIcon(Constants.NORMALSCREEN_ICON);
            stage.setFullScreen(true);
        }
    }

    private void setShowHidePopOver() {
        if (isShowing) {
            LOGGER.info("Hiding");
            isShowing = false;
            centerPane.setViewSwitchingButtonsVisible(false);
        } else {
            LOGGER.info("Showing");
            LOGGER.info("Layout Y before: " + centerPane.getPopUpChildBottom().getLayoutY());
            isShowing = true;
            centerPane.setViewSwitchingButtonsVisible(true);
            LOGGER.info("Layout Y after: " + centerPane.getPopUpChildBottom().getLayoutY());
        }
    }

    private void setChooseView(final ActionEvent event) {
        if (event.getSource().equals(centerPane.getPopUpChildTop())) {
            if (activeState.equals(State.SETTINGS)) {
                activeState = State.MOVEMENT;
            } else if (activeState.equals(State.MOVEMENT)) {
                activeState = State.TEMPERATURE;
            } else if (activeState.equals(State.TEMPERATURE)) {
                activeState = State.SETTINGS;
            }
        } else {
            if (activeState.equals(State.SETTINGS)) {
                activeState = State.TEMPERATURE;
            } else if (activeState.equals(State.MOVEMENT)) {
                activeState = State.SETTINGS;
            } else if (activeState.equals(State.TEMPERATURE)) {
                activeState = State.MOVEMENT;
            }

        }

        final String settingsIcon = "/icons/settings.png";
        final String temperatureIcon = "/icons/thermometer.png";
        final String movementIcon = "/icons/observe.png";
        if (activeState.equals(State.SETTINGS)) {
            centerPane.getPopUpParent().changeIcon(settingsIcon);
            centerPane.getPopUpChildBottom().changeIcon(temperatureIcon);
            centerPane.getPopUpChildTop().changeIcon(movementIcon);

        } else if (activeState.equals(State.MOVEMENT)) {
            centerPane.getPopUpParent().changeIcon(movementIcon);
            centerPane.getPopUpChildBottom().changeIcon(settingsIcon);
            centerPane.getPopUpChildTop().changeIcon(temperatureIcon);
        } else if (activeState.equals(State.TEMPERATURE)) {
            centerPane.getPopUpParent().changeIcon(temperatureIcon);
            centerPane.getPopUpChildBottom().changeIcon(movementIcon);
            centerPane.getPopUpChildTop().changeIcon(settingsIcon);
        }
    }
}
