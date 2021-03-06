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
package org.openbase.bco.bcozy.view.pane.unit;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.animation.Animation;
import javafx.scene.paint.Color;
import org.openbase.bco.dal.remote.layer.unit.BatteryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.type.domotic.unit.dal.BatteryDataType.BatteryData;

/**
 * Created by tmichalski on 13.01.16.
 */
public class BatteryPane extends AbstractUnitPane<BatteryRemote, BatteryData> {

    public BatteryPane() {
        super(BatteryRemote.class, false);
        this.setIcon(MaterialDesignIcon.BATTERY_OUTLINE, MaterialDesignIcon.BATTERY);
    }

    @Override
    public void updateDynamicContent() {
        super.updateDynamicContent();

        try {
            final double level = getUnitRemote().getData().getBatteryState().getLevel();
            final GlyphIcons icon;
            if (level > 0.90) {
                icon = MaterialDesignIcon.BATTERY;
            } else if (level > 0.80) {
                icon = MaterialDesignIcon.BATTERY_90;
            } else if (level > 0.70) {
                icon = MaterialDesignIcon.BATTERY_80;
            } else if (level > 0.60) {
                icon = MaterialDesignIcon.BATTERY_70;
            } else if (level > 0.50) {
                icon = MaterialDesignIcon.BATTERY_60;
            } else if (level > 0.40) {
                icon = MaterialDesignIcon.BATTERY_50;
            } else if (level > 0.30) {
                icon = MaterialDesignIcon.BATTERY_40;
            } else if (level > 0.20) {
                icon = MaterialDesignIcon.BATTERY_30;
            } else if (level > 0.10) {
                icon = MaterialDesignIcon.BATTERY_20;
            } else if (level > 0.05) {
                icon = MaterialDesignIcon.BATTERY_10;
            } else {
                icon = MaterialDesignIcon.BATTERY_ALERT;
            }
            getIcon().setBackgroundIcon(icon);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.DEBUG);
        }
        
        try {
            switch (getUnitRemote().getData().getBatteryState().getValue()) {
                case UNKNOWN:
                    getIcon().setBackgroundIconColor(Color.BLACK);
                    break;
                case OK:
                    getIcon().setBackgroundIconColor(Color.GREEN);
                      break;
                case CRITICAL:
                    getIcon().setBackgroundIconColor(Color.ORANGE);
                    getIcon().startBackgroundIconColorFadeAnimation(Animation.INDEFINITE);
                    break;
                case INSUFFICIENT:
                    getIcon().setBackgroundIconColor(Color.RED);
                    getIcon().startBackgroundIconColorFadeAnimation(Animation.INDEFINITE);
                    break;
                default:
                    break;
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            getIcon().setBackgroundIconColor(Color.BLACK);
        }
    }
}
