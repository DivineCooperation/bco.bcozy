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
package org.dc.bco.bcozy;

import com.guigarage.responsive.ResponsiveHandler;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.dc.bco.bcozy.controller.CenterPaneController;
import org.dc.bco.bcozy.controller.ContextMenuController;
import org.dc.bco.bcozy.controller.LocationController;
import org.dc.bco.bcozy.controller.MainMenuController;
import org.dc.bco.bcozy.controller.RemotePool;
import org.dc.bco.bcozy.view.BackgroundPane;
import org.dc.bco.bcozy.view.Constants;
import org.dc.bco.bcozy.view.ForegroundPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Class of the BCozy Program.
 */
public class BCozy extends Application {

    /**
     * Application name.
     */
    public static final String APP_NAME = BCozy.class.getSimpleName().toLowerCase();

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BCozy.class);

    private RemotePool remotePool;

    /**
     * Main Method starting JavaFX Environment.
     *
     * @param args Arguments from commandline.
     */
    public static void main(final String... args) {

        LOGGER.info("Start " + APP_NAME + "...");

        registerListeners();
        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.registerProperty(JPDebugMode.class);

        try {
            JPService.parseAndExitOnError(args);
            launch(args);
        } catch (IllegalStateException ex) {
//        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            LOGGER.info(APP_NAME + " finished unexpected.");
        }
        LOGGER.info(APP_NAME + " finished.");
    }

    @Override
    public void start(final Stage primaryStage) {

        // load the tron font.
        Font.loadFont(BCozy.class.getClassLoader().getResource("fonts/Roboto-Thin.ttf").toExternalForm(),
                Constants.STANDARD_TEXT_SIZE);

        final double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
        final double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        primaryStage.setTitle("BCozy");

        final StackPane root = new StackPane();
        final ForegroundPane foregroundPane = new ForegroundPane(screenHeight, screenWidth);
        foregroundPane.setMinHeight(root.getHeight());
        foregroundPane.setMinWidth(root.getWidth());
        final BackgroundPane backgroundPane = new BackgroundPane(foregroundPane);

        root.getChildren().addAll(backgroundPane, foregroundPane);

        //CHECKSTYLE.OFF: MagicNumber
//        primaryStage.setMinHeight(foregroundPane.getMenuHeader().getMinHeight()
//                + foregroundPane.getInfoFooter().getMinHeight() + 300);
        primaryStage.setMinWidth(foregroundPane.getMainMenu().getMinWidth()
                + foregroundPane.getContextMenu().getMinWidth() + 300);
        primaryStage.setHeight(screenHeight);
        //CHECKSTYLE.ON: MagicNumber

        primaryStage.setScene(new Scene(root, screenWidth, screenHeight));

        primaryStage.getScene().getStylesheets().add(BCozy.class.getClassLoader().
                getResource("css/skin.css").toExternalForm());
        ResponsiveHandler.addResponsiveToWindow(primaryStage);
        primaryStage.show();

        new MainMenuController(foregroundPane);
        new CenterPaneController(foregroundPane);

        //instantiate RemotePool
        remotePool = new RemotePool(foregroundPane);
        new ContextMenuController(foregroundPane, backgroundPane.getLocationPane(), remotePool);

        //instantiate LocationController
        try {
            new LocationController(foregroundPane, backgroundPane.getLocationPane(), remotePool);
        } catch (InstantiationException e) {
            ExceptionPrinter.printHistory(e, LOGGER, LogLevel.ERROR);
        }
    }

    @Override
    public void stop() throws Exception { //NOPMD
        super.stop();
        remotePool.shutdownAllRemotes(); //TODO mpohling: not shutting down properly
        System.exit(0);
    }

    private static void registerListeners() {
        LOGGER.info("Executing Registration of Listeners");
        ResponsiveHandler.setOnDeviceTypeChanged((over, oldDeviceType, newDeviceType) -> {
            switch (newDeviceType) {
                case LARGE      : adjustToLargeDevice();        break;
                case MEDIUM     : adjustToMediumDevice();       break;
                case SMALL      : adjustToSmallDevice();        break;
                case EXTRA_SMALL: adjustToExtremeSmallDevice(); break;
                default : break;
            }
        });
    }
    private static void adjustToLargeDevice() {
        LOGGER.info("Detected Large Device");
    }
    private static void adjustToMediumDevice() {
        LOGGER.info("Detected Medium Device");
    }
    private static void adjustToSmallDevice() {
        LOGGER.info("Detected Small Device");
    }
    private static void adjustToExtremeSmallDevice() {
        LOGGER.info("Detected Extreme Small Device");
    }
}
