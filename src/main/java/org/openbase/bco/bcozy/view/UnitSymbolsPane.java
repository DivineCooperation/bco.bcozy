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
 * along with org.openbase.bco.bcozy. If not, see
 * <http://www.gnu.org/licenses/>.
 * ==================================================================
 */
package org.openbase.bco.bcozy.view;

import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import org.openbase.bco.bcozy.view.location.DynamicUnitPolygon;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import org.openbase.bco.bcozy.view.location.UnitButton;
import org.openbase.bco.bcozy.view.location.UnitButtonGrouped;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;

/**
 * Pane for the top layer of the room plan that includes buttons for the light units.
 *
 * @author lili
 */
public class UnitSymbolsPane extends Pane {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitSymbolsPane.class);
    // locationId, room-level buttons
    private final Map<String, UnitButton> locationUnitsMap;
    // locationId, unitId, unit-level buttons
    private final Map<String, Map<String, UnitButton>> unitsPerLocationMap;
    // coordinates, grouped unit-level buttons
    private final Map<Point2D, UnitButtonGrouped> groupedButtons;
    private boolean putIntoGroup;
    public final SimpleObjectProperty<DynamicUnitPolygon<?,?>> selectedUnit;

    /**
     * Constructor for the UnitSymbolsPane.
     */
    public UnitSymbolsPane() {
        super();
        locationUnitsMap = new HashMap<>();
        unitsPerLocationMap = new HashMap<>();
        groupedButtons = new HashMap<>();
        selectedUnit = new SimpleObjectProperty<>();
        selectedUnit.addListener((ChangeListener<DynamicUnitPolygon>) (observable, oldPolygon, newPolygon) -> updateUnitsPane());
    }

    /**
     * Adds a new button for the control of a location.
     *
     * @param unitRemoteObject Unit to be controlled by the button.
     * @param position Position of the button on the map, should be the center of the location unit pane.
     * @throws CouldNotPerformException
     * @throws InterruptedException
     */
    public void addLocationUnit(final UnitRemote<? extends Message> unitRemoteObject, final Point2D position) throws CouldNotPerformException, InterruptedException {
        UnitButton newButton;
        try {
            newButton = new UnitButton(unitRemoteObject);

            newButton.setTranslateX(position.getY());  //swap according to swap in location pane 
            newButton.setTranslateY(position.getX());
            locationUnitsMap.put(unitRemoteObject.getConfig().getId(), newButton);
        } catch (NotAvailableException ex) {
            throw new CouldNotPerformException("Could not create unit button for unit " + this, ex);
        }
    }

    /**
     * Adds a new button for the control of a unit or adds a unit to an existing grouped button.
     *
     * @param unitRemoteObject Unit to be controlled by the button.
     * @param position Position of the button on the map, should be the center of the location unit pane.
     * @param locationId Id of the location the unit belongs to.
     * @throws CouldNotPerformException
     * @throws InterruptedException
     */
    public void addUnit(final UnitRemote<? extends Message> unitRemoteObject, final Point2D position, final String locationId) throws CouldNotPerformException, InterruptedException {
        UnitButton newButton;
        try {
            newButton = new UnitButton(unitRemoteObject);

            newButton.setTranslateX(position.getY());
            newButton.setTranslateY(position.getX());

            putIntoGroup = false;
            String toDelete;

            // room unit for this location already exists in the map 
            if (unitsPerLocationMap.containsKey(locationId)) {

                Point2D coord = new Point2D(position.getX(), position.getY());
                // grouped button already exists
                if (groupedButtons.containsKey(coord)) {
                    groupedButtons.get(coord).addUnit(unitRemoteObject);
                    putIntoGroup = true;

                    // grouped button needs to be initalized
                } else {
                    Iterator<Map.Entry<String, UnitButton>> iter = unitsPerLocationMap.get(locationId).entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<String, UnitButton> entry = iter.next();
                        UnitButton button = entry.getValue();

                        if (button.getTranslateX() == position.getY() && button.getTranslateY() == position.getX()) {
                            UnitButtonGrouped newGroupedButton = new UnitButtonGrouped();
                            newGroupedButton.setTranslateX(position.getY());
                            newGroupedButton.setTranslateY(position.getX());
                            groupedButtons.put(new Point2D(position.getX(), position.getY()), newGroupedButton);
                            newGroupedButton.addUnit(unitRemoteObject);
                            newGroupedButton.addUnit(button.getUnitRemote());
                            // remove from normal buttons list to prevent double buttons
                            iter.remove();
                            putIntoGroup = true;
                        }
                    }
                }
                if (!putIntoGroup) {
                    unitsPerLocationMap.get(locationId).put(unitRemoteObject.getConfig().getId(), newButton);
                }

                // entry in the map for this location needs to be initalized
            } else {
                Map<String, UnitButton> units = new HashMap<>();
                units.put(unitRemoteObject.getConfig().getId(), newButton);
                unitsPerLocationMap.put(locationId, units);
            }
        } catch (NotAvailableException ex) {
            throw new CouldNotPerformException("Could not create unit button for unit " + this, ex);
        }
    }

    /**
     * Clears the UnitSymbolsPane to prepare the update.
     */
    public void clearUnits() {
        locationUnitsMap.forEach((unitId, button)
                -> {
            this.getChildren().remove(button);
        });
        locationUnitsMap.clear();
        unitsPerLocationMap.forEach((locationId, entry)
                -> entry.forEach((unitId, button)
                        -> {
                    this.getChildren().remove(button);
                })
        );
        unitsPerLocationMap.clear();
        groupedButtons.forEach((point, button)
                -> {
            this.getChildren().remove(button);
        });
        groupedButtons.clear();
    }

    /**
     * Draws all location buttons except for the selected location, draws all unit buttons
     * and grouped buttons for the selected location.
     */
    public void updateUnitsPane() {
        this.getChildren().clear();

        if (selectedUnit.get() == null) {
            return;
        }

        try {
            final String selectedUnitID = selectedUnit.get().getUnitId();

            locationUnitsMap.forEach((unitId, button)
                    -> {
                if (!unitId.equals(selectedUnitID)) {
                    this.getChildren().add(button);
                }
            });

            // do not show icons in edit mode
            if (selectedUnit.get().isEditModeEnabled()) {
                return;
            }

            if (unitsPerLocationMap.get(selectedUnitID) != null) {
                unitsPerLocationMap.get(selectedUnitID).forEach((unitId, button)
                                -> {
                            this.getChildren().add(button);
                        }
                );
            }
            groupedButtons.forEach((point, button)
                    -> {
                if (button.getLocationId() == null) {
                    return;
                }
                if (button.getLocationId().equals(selectedUnitID)) {
                    this.getChildren().add(button);
                }
            });

        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not update units pane.", ex, LOGGER);
        }


    }
}
