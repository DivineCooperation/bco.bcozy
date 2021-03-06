package org.openbase.bco.bcozy.view.pane.unit.location;

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
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.scene.paint.Color;
import org.openbase.bco.bcozy.view.Constants;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.schedule.RecurrenceEventFilter;
import org.openbase.jul.visual.javafx.JFXConstants;
import org.openbase.jul.visual.javafx.geometry.svg.SVGGlyphIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Future;
import javafx.scene.layout.Pane;
import org.openbase.bco.bcozy.view.generic.ColorChooser;
import org.openbase.bco.bcozy.view.pane.unit.AbstractUnitPane;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.jul.visual.javafx.transform.JFXColorToHSBColorTransformer;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;

/**
 * Created by agatting on 03.12.15.
 */
public class LocationPane extends AbstractUnitPane<LocationRemote, LocationData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationPane.class);

    private ColorChooser colorChooser;

    private final RecurrenceEventFilter<Color> recurrenceEventFilterHSV = new RecurrenceEventFilter<Color>(Constants.RECURRENCE_EVENT_FILTER_MILLI_TIMEOUT) {

        @Override
        public void relay() {
            try {
                getUnitRemote().setColor(JFXColorToHSBColorTransformer.transform(getLatestValue()));
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not send color update!", ex, LOGGER);
            }
        }
    };

    /**
     * Constructor for the Location Pane.
     *
     */
    public LocationPane() {
        super(LocationRemote.class, true);
        this.setIcon(MaterialDesignIcon.LIGHTBULB_OUTLINE, MaterialDesignIcon.LIGHTBULB);
    }

    @Override
    protected void initBodyContent(Pane bodyPane) throws CouldNotPerformException {
        colorChooser = new ColorChooser();
        colorChooser.initContent();
        colorChooser.selectedColorProperty().addListener((observable) -> {
            try {
                recurrenceEventFilterHSV.trigger(colorChooser.getSelectedColor());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not trigger color change!", ex, LOGGER);
            }
        });
        bodyPane.getChildren().add(colorChooser);

    }

    @Override
    public void updateDynamicContent() {
        super.updateDynamicContent();

        // detect power state
        PowerState.State state = PowerState.State.UNKNOWN;
        try {
            state = getUnitRemote().getData().getPowerState().getValue();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.DEBUG);
        }

        // detect color
        Color color;
        try {
            color = JFXColorToHSBColorTransformer.transform(getData().getColorState().getColor().getHsbColor(), getData().getColorState().getColor().getHsbColor().getBrightness());
        } catch (CouldNotPerformException ex) {
            color = Constants.LIGHTBULB_OFF_COLOR;
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.DEBUG);
        }

        if (colorChooser != null && expansionProperty().get()) {
            colorChooser.setSelectedColor(color);
        }

        switch (state) {
            case OFF:
                getIcon().setBackgroundIconColor(Constants.LIGHTBULB_OFF_COLOR);
                setInfoText("lightOff");
                setPrimaryActivationWithoutNotification(Boolean.FALSE);
                break;
            case ON:
                getIcon().setBackgroundIconColor(color);
                setInfoText("lightOn");
                setPrimaryActivationWithoutNotification(Boolean.TRUE);
                break;
            default:
                setInfoText("unknown");
                break;
        }
    }

    @Override
    protected Future applyPrimaryActivationUpdate(final boolean activation) throws CouldNotPerformException {

        return (activation) ? getUnitRemote().setPowerState(PowerState.State.ON, UnitType.LIGHT)
            : getUnitRemote().setPowerState(PowerState.State.OFF, UnitType.LIGHT);
    }

    @Override
    public SVGGlyphIcon getIconSymbol() {
        return new SVGGlyphIcon(MaterialDesignIcon.LIGHTBULB, JFXConstants.ICON_SIZE_SMALL, false);
    }
}
