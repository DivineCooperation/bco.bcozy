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

import de.citec.dal.remote.unit.DALRemoteService;
import de.citec.jul.exception.CouldNotPerformException;
import org.dc.bco.bcozy.view.ForegroundPane;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by timo on 03.12.15.
 */
public class ContextMenuController {
    private final ForegroundPane foregroundPane;
    private final RemotePool remotePool;

    /**
     * Constructor for the ContextMenuController.
     * @param foregroundPane foregroundPane
     * @param remotePool remotePool
     */
    public ContextMenuController(final ForegroundPane foregroundPane, final RemotePool remotePool) {
        this.foregroundPane = foregroundPane;
        this.remotePool = remotePool;
    }

    /**
     * Takes a locationId and creates new TitledPanes for all UnitTypes.
     * @param locationID locationID
     * @throws CouldNotPerformException CouldNotPerformException
     */
    public void setContextMenuDevicePanes(final String locationID) throws CouldNotPerformException {
        final Map<UnitType, List<DALRemoteService>> unitRemoteMap =
                remotePool.getUnitRemoteMapOfLocation(locationID);

        final Iterator<Map.Entry<UnitType, List<DALRemoteService>>> entryIterator =
                unitRemoteMap.entrySet().iterator();

        while (entryIterator.hasNext()) {
            final Map.Entry<UnitType, List<DALRemoteService>> nextEntry =
                    entryIterator.next();

            foregroundPane.getContextMenu().getTitledPaneContainer().createAndAddNewTitledPane(
                    nextEntry.getKey(), nextEntry.getValue());
        }
    }
}
