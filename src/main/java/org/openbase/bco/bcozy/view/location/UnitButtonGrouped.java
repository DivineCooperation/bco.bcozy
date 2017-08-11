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
package org.openbase.bco.bcozy.view.location;

import com.google.protobuf.GeneratedMessage;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.openbase.bco.bcozy.view.SVGIcon;
import org.openbase.bco.bcozy.view.generic.WidgetPane.DisplayMode;
import org.openbase.bco.bcozy.view.pane.unit.AbstractUnitPane;
import org.openbase.bco.bcozy.view.pane.unit.UnitPaneFactoryImpl;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;

/**
 *
 */
public class UnitButtonGrouped extends Pane {

    // private ObservableList<AbstractUnitPane> unitButtons;
    private SimpleListProperty<AbstractUnitPane> unitButtons;
    private final FlowPane groupingPane;
    private final StackPane stackPane;
    private final Label unitCount;
    boolean expanded;

    public UnitButtonGrouped() {
        expanded = false;
        groupingPane = new FlowPane(Orientation.HORIZONTAL);
        stackPane = new StackPane();
        unitCount = new Label("0");
        unitCount.setTextAlignment(TextAlignment.LEFT);
        unitCount.setFont(new Font(12));
        unitButtons = new SimpleListProperty(FXCollections.<AbstractUnitPane>observableArrayList());
        unitCount.textProperty().bind(unitButtons.sizeProperty().asString());

        stackPane.getChildren().add(groupingPane);
        stackPane.getChildren().add(unitCount);
        stackPane.setAlignment(unitCount, Pos.BOTTOM_LEFT);

        final EventHandler<MouseEvent> mouseEventHandler = (MouseEvent event) -> {
            event.consume();
            if (!expanded) {
                expand();
                expanded = true;
            } 
        };
        
        final EventHandler<MouseEvent> mouseExitedHandler = (MouseEvent event) -> {
            event.consume();
            if (expanded) {
                shrink();
                expanded = false;
                System.out.println("out focus");
            } 
        };
/*
        this.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    System.out.println("on focus");
                } else {
                    System.out.println("out focus");
                }
            }
        });*/

        groupingPane.setOnMouseClicked(mouseEventHandler);
        groupingPane.setOnMouseExited(mouseExitedHandler);
        unitCount.setOnMouseClicked(mouseEventHandler);

        this.getChildren().add(stackPane);
    }

    public void addUnit(UnitRemote<? extends GeneratedMessage> unit) {

        try {
            AbstractUnitPane content;
            content = UnitPaneFactoryImpl.getInstance().newInitializedInstance(unit.getConfig());
            content.setDisplayMode(DisplayMode.ICON_ONLY);
            SVGIcon icon = content.getIcon();
            if (unitButtons.isEmpty()) {
                groupingPane.getChildren().add(icon);
            }
            unitButtons.add(content);
        } catch (CouldNotPerformException | InterruptedException ex) {
            Logger.getLogger(UnitButtonGrouped.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void expand() {
        stackPane.getChildren().remove(unitCount);
        unitButtons.forEach((button)
            -> {
            groupingPane.getChildren().add(button);
        });
       // this.stackPane.getChildren().remove(unitCount);
      //  this.getChildren().add(stackPane);
    }

    public void shrink() {
        this.groupingPane.getChildren().clear();
        stackPane.getChildren().add(unitCount);
    }
    
    //TODO refactor
      public UnitRemote<? extends GeneratedMessage> getUnitRemote() {
        try {
            return this.unitButtons.get(0).getUnitRemote();
        } catch (NotAvailableException ex) {
            Logger.getLogger(UnitButtonGrouped.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /*  count.setValue(count.getValue() + 1);

    final ContextMenu cm = new ContextMenu();
            MenuItem cmItem1 = new MenuItem("Toggle power state");
            try {
                UnitPaneFactoryImpl.getInstance().newInstance(UnitPaneFactoryImpl.loadUnitPaneClass(config.getType()));
                CustomMenuItem cmItem2 = new CustomMenuItem();
                cm.getItems().add(cmItem2);
            } catch (CouldNotPerformException ex) {
                Logger.getLogger(UnitButton.class.getName()).log(Level.SEVERE, null, ex);
            }
            cmItem1.setOnAction((ActionEvent e) -> {
                //
            });
            cm.getItems().add(cmItem1);
            this.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent e) -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    cm.show(this.getParent(), e.getScreenX(), e.getScreenY());
                }
            });
 
     */
}
