/**
 * ==================================================================
 *
 * This file is part of org.openbase.bco.bcozy.
 *
 * org.openbase.bco.bcozy is free software: you can redistribute it and modify
 * it under the terms of the GNU General Public License (Version 3)
 * as published by the Free Software Foundation.
 *
 * org.openbase.bco.bcozy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with org.openbase.bco.bcozy. If not, see <http://www.gnu.org/licenses/>.
 * ==================================================================
 */
package org.openbase.bco.bcozy.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javax.vecmath.Point3d;
import org.openbase.bco.bcozy.view.Constants;
import org.openbase.bco.bcozy.view.UnitSymbolsPane;
import org.openbase.bco.bcozy.view.location.LocationMapPane;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.visual.javafx.JFXConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.rct.Transform;
import org.openbase.type.domotic.state.EnablingStateType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.location.LocationConfigType;
import org.openbase.type.geometry.AxisAlignedBoundingBox3DFloatType;
import org.openbase.type.geometry.PoseType;

/**
 * Controller for the top layer of the room plan that includes buttons for the light units.
 *
 * @author lili
 */
public class UnitsPaneController {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitsPaneController.class);

    private final LocationMapPane locationMapPane;
    private final UnitSymbolsPane unitSymbolsPane;

    /**
     * Constructor
     *
     * @param unitPane
     * @param locationMapPane
     */
    public UnitsPaneController(final UnitSymbolsPane unitPane, final LocationMapPane locationMapPane) {
        this.locationMapPane = locationMapPane;
        this.unitSymbolsPane = unitPane;

        unitPane.scaleXProperty().bind(locationMapPane.scaleXProperty());
        unitPane.scaleYProperty().bind(locationMapPane.scaleYProperty());
        unitPane.translateXProperty().bind(locationMapPane.translateXProperty());
        unitPane.translateYProperty().bind(locationMapPane.translateYProperty());
    }

    /**
     * Establish the connection with the RemoteRegistry and fetch unit remotes.
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException
     * @throws java.lang.InterruptedException
     */
    public void connectUnitRemote() throws CouldNotPerformException, InterruptedException {
        try {
            Registries.waitForData();
            Registries.getUnitRegistry().addDataObserver((source, data) -> Platform.runLater(() -> {
                try {
                    fetchLocationUnitRemotes();
                    unitSymbolsPane.updateUnitsPane();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }));
            Registries.getUnitRegistry().addDataObserver((source, data) -> Platform.runLater(() -> {
                try {
                    fetchLocationUnitRemotes();
                    unitSymbolsPane.updateUnitsPane();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }));
            updateUnits();
        } catch (CouldNotPerformException ex) { //NOPMD
            throw new CouldNotPerformException("Could not fetch units from remote registry", ex);
        }
    }

    /**
     * Fetches all location units, saves them in the UnitSymbolsPane and then
     * fetches all units for every location and saves them also in the UnitSymbolsPane.
     *
     * @throws CouldNotPerformException
     * @throws InterruptedException
     */
    public void fetchLocationUnitRemotes() throws CouldNotPerformException, InterruptedException {

        unitSymbolsPane.clearUnits();

        unitSymbolsPane.clearUnits();

        final double halfButtonSize = (JFXConstants.ICON_SIZE_SMALL + (2 * Constants.INSETS)) / 2;

        final List<UnitConfig> locationUnitConfigList = Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.LOCATION);

        for (final UnitConfig locationConfig : locationUnitConfigList) {

            // Tiles are the clickable polygons 
            if (locationConfig.getLocationConfig().getLocationType() != LocationConfigType.LocationConfig.LocationType.TILE) {
                continue;
            }
            // Only use locations with a valuable shape
            if (locationConfig.getPlacementConfig().getShape().getFloorCount() == 0) {
                continue;
            }

            Point3d vertex = calculateCoordinates(locationConfig);

            try {
                final Future<Transform> transform = Registries.getUnitRegistry().getUnitTransformation(locationConfig, Registries.getUnitRegistry().getRootLocationConfig());
                transform.get(Constants.TRANSFORMATION_TIMEOUT / 10, TimeUnit.MILLISECONDS).getTransform().transform(vertex);
                Point2D coord = new Point2D(vertex.x * Constants.METER_TO_PIXEL, vertex.y * Constants.METER_TO_PIXEL);
                // Abstract Pane not working with a config object, only with a remote one!
                UnitRemote<?> u = Units.getUnit(locationConfig.getId(), false);
                unitSymbolsPane.addLocationUnit(u, coord.add(-halfButtonSize, -halfButtonSize));
            } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
                // No exception throwing, because loop must continue it's work
                ExceptionPrinter.printHistory(ex, LOGGER);
            }

            for (final Map.Entry<UnitTemplateType.UnitTemplate.UnitType, List<UnitRemote>> nextEntry : Units.getUnit(locationConfig.getId(), false, Units.LOCATION).getUnitMap().entrySet()) {
                if (nextEntry.getValue().isEmpty()) {
                    continue;
                }

                for (UnitRemote<?> unitRemote : nextEntry.getValue()) {
                    if (nextEntry.getKey() != UnitType.COLORABLE_LIGHT &&
                        nextEntry.getKey() != UnitType.DIMMABLE_LIGHT &&
                        nextEntry.getKey() != UnitType.LIGHT &&
                        nextEntry.getKey() != UnitType.POWER_SWITCH) {
                        continue;
                    }

                    UnitConfig config = unitRemote.getConfig();
                    if (config.getEnablingState().getValue() != EnablingStateType.EnablingState.State.ENABLED) {
                        continue;
                    }
                    if (!config.getPlacementConfig().hasPose()) {
                        continue;
                    }

                    PoseType.Pose pose = config.getPlacementConfig().getPose();
                    try {
                        final Future<Transform> transform = Registries.getUnitRegistry().getUnitTransformation(config,
                                Registries.getUnitRegistry().getRootLocationConfig());
                        // transformation already in unit's coordinate space, therefore the zeros
                        final Point3d unitVertex = new Point3d(0.0, 0.0, 1.0);
                        transform.get(Constants.TRANSFORMATION_TIMEOUT / 10, TimeUnit.MILLISECONDS).
                                getTransform().transform(unitVertex);
                        Point2D coord = new Point2D(unitVertex.x * Constants.METER_TO_PIXEL, unitVertex.y * Constants.METER_TO_PIXEL);
                        // correction of position necessary because:
                        // "pose" is left bottom of unit bounding box (y correction) and the unit button's center 
                        // should be at the unit position (x correction) Attention: X and Y swapped in UnitButton 
                        unitSymbolsPane.addUnit(unitRemote, coord.add(-0.5 * halfButtonSize, -halfButtonSize), locationConfig.getId());

                    } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
                        // No exception throwing, because loop must continue it's work
                        ExceptionPrinter.printHistory(ex, LOGGER);

                    }
                }
            }
        }
    }

    private Point3d calculateCoordinates(final UnitConfig locationConfig) {
        AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat boundingBox
                = locationConfig.getPlacementConfig().getShape().getBoundingBox();

        double d = boundingBox.getDepth();
        double w = boundingBox.getWidth();
        double new_x;
        double new_y;

        new_x = (boundingBox.getLeftFrontBottom().getX() + w) / 2;
        new_y = (boundingBox.getLeftFrontBottom().getY() + d) / 2;

        return new Point3d(new_x, new_y, 1.0);
    }

    /**
     * Fetches all unit remotes from registry and updates the unit pane,
     * so all unit buttons represent the correct configuration.
     */
    public void updateUnits() {
        Platform.runLater((() -> {
            try {
                fetchLocationUnitRemotes();
                unitSymbolsPane.updateUnitsPane();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }));
    }
}
