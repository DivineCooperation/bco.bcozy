package org.openbase.bco.bcozy.permissions;

import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import org.controlsfx.control.textfield.CustomTextField;
import org.openbase.bco.bcozy.permissions.model.RecursiveUnitConfig;
import org.openbase.bco.bcozy.view.ObserverLabel;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.visual.javafx.JFXConstants;
import org.openbase.jul.visual.javafx.control.AbstractFXController;
import org.openbase.jul.visual.javafx.geometry.svg.SVGGlyphIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.language.MultiLanguageTextType;
import org.openbase.type.language.LabelType;
import org.openbase.type.language.LabelType.Label.MapFieldEntry;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.nonNull;

/**
 * Controller for selecting unit to edit permissions for.
 * Permissions of the selected group are edited wih subcontroller {@link UnitPermissionController}.
 *
 * @author vdasilva
 */
public class PermissionsPaneController extends AbstractFXController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsPaneController.class);

    @FXML
    private Parent unitPermission;

    @FXML
    private UnitPermissionController unitPermissionController;

    @FXML
    private CustomTextField filterInput;

    @FXML
    private JFXTreeTableView<RecursiveUnitConfig> unitsTable;
    @FXML
    private JFXTreeTableColumn<RecursiveUnitConfig, String> typeColumn;
    @FXML
    private JFXTreeTableColumn<RecursiveUnitConfig, String> descColumn;
    @FXML
    private JFXTreeTableColumn<RecursiveUnitConfig, String> labelColumn;

    private final ObservableList<RecursiveUnitConfig> list = FXCollections.observableArrayList();

    @Override
    public void initContent() throws InitializationException {
        fillTreeTableView();

        try {
            Registries.getUnitRegistry().addDataObserver((o, unitRegistryData) -> fillTable());
            fillTable();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }

    @Override
    public void updateDynamicContent() {

    }

    @FXML
    public void initialize() {
        fillTreeTableView();

        try {
            Registries.getUnitRegistry().addDataObserver((o, unitRegistryData) -> fillTable());
            fillTable();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }

    private void onSelectionChange(javafx.beans.Observable observable, TreeItem oldValue, TreeItem newValue) {
        if (nonNull(newValue) && newValue.getValue() instanceof RecursiveUnitConfig) {
            setUnitPermissionVisible(true);
            unitPermissionController.setSelectedUnitId(((RecursiveUnitConfig) newValue.getValue()).getUnit().getId());
        } else {
            setUnitPermissionVisible(false);
        }
    }

    private void setUnitPermissionVisible(boolean visible) {
        unitPermission.setVisible(visible);
    }

    private void fillTreeTableView() {
        labelColumn.setCellValueFactory(new MethodRefCellValueFactory<>((unit) -> LabelProcessor.getBestMatch(unit.getUnit().getLabel(),""), labelColumn));

        descColumn.setCellValueFactory(new MethodRefCellValueFactory<>((unit) -> MultiLanguageTextProcessor.getBestMatch(unit.getUnit().getDescription(),""), descColumn));

        typeColumn.setCellValueFactory(new MethodRefCellValueFactory<>((unit) -> unit.getUnit().getUnitType().name(), typeColumn));

        RecursiveTreeItem<RecursiveUnitConfig> item = new RecursiveTreeItem<>(list, RecursiveTreeObject::getChildren);
        unitsTable.setRoot(item);

        unitsTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(this::onSelectionChange);

        filterInput.setRight(new SVGGlyphIcon(FontAwesomeIcon.SEARCH, JFXConstants.ICON_SIZE_EXTRA_SMALL, true));

        filterInput.promptTextProperty().setValue(new ObserverLabel("searchPlaceholder").getText());
        filterInput.textProperty().addListener((o, oldVal, newVal) -> {
            unitsTable.setPredicate(user ->
                    detectMatch(newVal.toLowerCase(), user.getValue().getUnit())
                    || user.getValue().getUnit().getUnitType().name().toLowerCase().contains(newVal.toLowerCase()));
            });
    }

    private static boolean detectMatch(String key, UnitConfig unitConfig) {
        for (MapFieldEntry mapFieldEntry : unitConfig.getLabel().getEntryList()) {
            if(mapFieldEntry.getValueList().contains(key)) {
                return true;
            }
        }
        return unitConfig.getAliasList().contains(key) || unitConfig.getDescription().getEntryList().contains(key);
    }

    private class MethodRefCellValueFactory<S, T> implements Callback<TreeTableColumn.CellDataFeatures<S, T>, ObservableValue<T>> {

        Function<S, T> supplier;

        JFXTreeTableColumn<S, T> column;

        public MethodRefCellValueFactory(Function<S, T> supplier, JFXTreeTableColumn<S, T> column) {
            this.supplier = Objects.requireNonNull(supplier);
            this.column = Objects.requireNonNull(column);
        }

        @Override
        public ObservableValue<T> call(TreeTableColumn.CellDataFeatures<S, T> param) {
            if (column.validateValue(param)) {
                return new SimpleObjectProperty<>(supplier.apply(param.getValue().getValue()));
            }
            return column.getComputedValue(param);
        }
    }

    private void fillTable() throws CouldNotPerformException {
        if (Registries.getUnitRegistry().isDataAvailable()) {
            List<UnitConfigType.UnitConfig> unitConfigList = Registries.getUnitRegistry().getUnitConfigs();
            Platform.runLater(() -> fillTable(unitConfigList));
        }
    }

    private void fillTable(List<UnitConfigType.UnitConfig> unitConfigList) {

        unitsTable.unGroup(this.typeColumn);

        list.clear();

        for (UnitConfigType.UnitConfig unitConfig : unitConfigList) {
            if (nonNull(unitConfig)) {
                list.add(new RecursiveUnitConfig(unitConfig));
            }
        }

        if (!list.isEmpty()) {
            unitsTable.group(this.typeColumn);
        }
    }

}
