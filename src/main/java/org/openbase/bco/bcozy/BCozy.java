/**
 * ==================================================================
 * <p>
 * This file is part of org.openbase.bco.bcozy.
 * <p>
 * org.openbase.bco.bcozy is free software: you can redistribute it and modify
 * it under the terms of the GNU General Public License (Version 3)
 * as published by the Free Software Foundation.
 * <p>
 * org.openbase.bco.bcozy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with org.openbase.bco.bcozy. If not, see <http://www.gnu.org/licenses/>.
 * ==================================================================
 */
package org.openbase.bco.bcozy;

import com.guigarage.responsive.ResponsiveHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.openbase.bco.bcozy.controller.*;
import org.openbase.bco.bcozy.controller.powerterminal.PowerTerminalSidebarPaneController;
import org.openbase.bco.bcozy.jp.JPFullscreenMode;
import org.openbase.bco.bcozy.util.ThemeManager;
import org.openbase.bco.bcozy.view.BackgroundPane;
import org.openbase.bco.bcozy.view.ForegroundPane;
import org.openbase.bco.bcozy.view.InfoPane;
import org.openbase.bco.bcozy.view.LoadingPane;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.controller.Remote;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author hoestreich
 * @author timo
 * @author agatting
 * @author julian
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * <p>
 * Main Class of the BCozy Program.
 */
public class BCozy extends Application {

    public static SimpleObjectProperty<LocationRemote> selectedLocationProperty = new SimpleObjectProperty<>();
    public static SimpleObjectProperty<CenterPaneController.State> appModeProperty = new SimpleObjectProperty<>(CenterPaneController.State.MOVEMENT);

    /**
     * Application name.
     */
    public static final String APP_NAME = BCozy.class.getSimpleName().toLowerCase();

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BCozy.class);

    public static Stage primaryStage;
    private static Observer<Remote<?>, ConnectionState.State> connectionObserver;
    private LoadingPane loadingPane;
    private ContextMenuController contextMenuController;
    private LocationMapPaneController locationMapPaneController;
    private ForegroundPane foregroundPane;
    private UnitsPaneController unitsPaneController;
    private MaintenanceLayerController maintenanceLayerController;
    private EditingLayerController editingLayerController;
    private PowerTerminalSidebarPaneController sidebarPaneController;
    private Future<Void> initTask;
    private Scene mainScene;

    public BCozy() {

        connectionObserver = (source, data) -> {
            switch (data) {
                case CONNECTED:
                    // recover default
                    InfoPane.confirmation("connected");
                    break;
                case CONNECTING:
                    // green
                    InfoPane.warn("connecting");
                    break;
                case DISCONNECTED:
                    InfoPane.error("disconnected");
                    // red
                    break;
                case UNKNOWN:
                default:
                    // blue
                    break;
            }
        };
    }

    private static void registerResponsiveHandler() {
        LOGGER.debug("Register responsive handler...");
        ResponsiveHandler.setOnDeviceTypeChanged((over, oldDeviceType, newDeviceType) -> {
            switch (newDeviceType) {
                case LARGE:
                    adjustToLargeDevice();
                    break;
                case MEDIUM:
                    adjustToMediumDevice();
                    break;
                case SMALL:
                    adjustToSmallDevice();
                    break;
                case EXTRA_SMALL:
                    adjustToExtremeSmallDevice();
                    break;
                default:
                    break;
            }
        });
    }

    private static void adjustToLargeDevice() {
        LOGGER.debug("Detected Large Device");
    }

    private static void adjustToMediumDevice() {
        LOGGER.debug("Detected Medium Device");
    }

    private static void adjustToSmallDevice() {
        LOGGER.debug("Detected Small Device");
    }

    private static void adjustToExtremeSmallDevice() {
        LOGGER.debug("Detected Extreme Small Device");
    }

    @Override
    public void start(final Stage primaryStage) throws InitializationException, InterruptedException, InstantiationException {
        try {
            BCozy.primaryStage = primaryStage;
            registerResponsiveHandler();

            // set initial fullscreen if required
            primaryStage.setFullScreen(JPService.getValue(JPFullscreenMode.class, true));

            final double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
            final double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
            primaryStage.setTitle("BCozy");

            try {
                LOGGER.debug("Try to load icon...");
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/bco_logo_black_white.png")));
                LOGGER.debug("App icon loaded...");
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
            }


            final StackPane root = new StackPane();
            foregroundPane = new ForegroundPane(screenHeight, screenWidth);
            foregroundPane.setMinHeight(root.getHeight());
            foregroundPane.setMinWidth(root.getWidth());
            final BackgroundPane backgroundPane = new BackgroundPane(foregroundPane);

            loadingPane = new LoadingPane(screenHeight, screenWidth);
            loadingPane.setMinHeight(root.getHeight());
            loadingPane.setMinWidth(root.getWidth());
            root.getChildren().addAll(backgroundPane, foregroundPane, loadingPane);

            primaryStage.setMinWidth(foregroundPane.getMainMenu().getMinWidth() + foregroundPane.getUnitMenu().getMinWidth() + 300);
            primaryStage.setHeight(screenHeight);
            mainScene = new Scene(root, screenWidth, screenHeight);
            primaryStage.setScene(mainScene);

            ThemeManager.getInstance().loadDefaultTheme();

            new MainMenuController(foregroundPane);
            new CenterPaneController(foregroundPane);

            contextMenuController = new ContextMenuController(foregroundPane, backgroundPane.getLocationMapPane());
            locationMapPaneController = new LocationMapPaneController(backgroundPane.getLocationMapPane());
            unitsPaneController = new UnitsPaneController(backgroundPane.getUnitsPane(), backgroundPane.getLocationMapPane());
            maintenanceLayerController = new MaintenanceLayerController(backgroundPane.getMaintenancePane(), backgroundPane.getLocationMapPane());
            editingLayerController = new EditingLayerController(backgroundPane.getEditingPane(), backgroundPane.getLocationMapPane());

            try {
                sidebarPaneController = contextMenuController.getPowerTerminalSidebarPaneController();
                backgroundPane.initPowerTerminalPane(sidebarPaneController.getChartStateModel());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Power terminal initialization failed!", ex, LOGGER, LogLevel.WARN);
            }

            ResponsiveHandler.addResponsiveToWindow(primaryStage);
            primaryStage.show();

            InfoPane.confirmation("WELCOME");
            try {
                Registries.getUnitRegistry().addConnectionStateObserver(connectionObserver);
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory("Could not register bco connection observer!", ex, LOGGER);
            }

            initRemotesAndLocation();
        } catch (final Exception ex) {
            ExceptionPrinter.printHistory("Could not start " + JPService.getApplicationName(), ex, LOGGER);
        }
    }

    private void initRemotesAndLocation() {
        initTask = GlobalCachedExecutorService.submit(() -> {
            try {
                loadingPane.info("waitForConnection");
                Registries.waitForData();

                loadingPane.info("fillContextMenu");
                foregroundPane.init();

                contextMenuController.initTitledPaneMap();

                loadingPane.info("connectLocationRemote");
                locationMapPaneController.init();
                unitsPaneController.connectUnitRemote();
                maintenanceLayerController.connectUnitRemote();
                editingLayerController.connectUnitRemote();
                loadingPane.info("done");
                Platform.runLater(() -> {
                    loadingPane.setVisible(false);
                });
                return null;
            } catch (InterruptedException | CancellationException ex) {
                // init canceled.
                return null;
            } catch (Exception ex) {
                loadingPane.error("errorDuringStartup");
                Thread.sleep(3000);
                Exception exx = new FatalImplementationErrorException("Could not init panes", this, ex);
                ExceptionPrinter.printHistoryAndExit(exx, LOGGER);
                return null;
            }
        });
    }

    @Override
    public void stop() {
        boolean errorOccured = false;

        if (initTask != null && !initTask.isDone()) {
            initTask.cancel(true);
            try {
                initTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                ExceptionPrinter.printHistory("Initialization phase canceled because of application shutdown.", ex, LOGGER, LogLevel.INFO);
                errorOccured = true;
            } catch (CancellationException ex) {
                ExceptionPrinter.printHistory("Initialization phase failed but application shutdown was initialized anyway.", ex, LOGGER, LogLevel.WARN);
            }
        }

        try {
            Registries.getUnitRegistry().removeConnectionStateObserver(connectionObserver);
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not remove bco connection observer!", ex, LOGGER);
        }

        try {
            super.stop();
        } catch (Exception ex) { //NOPMD
            ExceptionPrinter.printHistory("Could not stop " + JPService.getApplicationName() + "!", ex, LOGGER);
            errorOccured = true;
        }

        // Call system exit to trigger all shutdown deamons.
        if (errorOccured) {
            System.exit(255);
        }
        System.exit(0);
    }

    @Override
    public void init() throws Exception {
        super.init();
        BCOLogin.getSession().autoLogin(false);
    }
}
