package org.openbase.bco.bcozy.controller.group;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.openbase.bco.bcozy.controller.ButtonTableCellFactory;
import org.openbase.bco.bcozy.controller.Dialog;
import org.openbase.bco.bcozy.model.LanguageSelection;
import org.openbase.bco.bcozy.util.AuthorizationGroups;
import org.openbase.bco.bcozy.util.ExceptionHelper;
import org.openbase.bco.bcozy.view.Constants;
import org.openbase.bco.bcozy.view.InfoPane;
import org.openbase.bco.bcozy.view.ObserverButton;
import org.openbase.bco.bcozy.view.SVGIcon;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType;

import java.util.Optional;

/**
 * @author vdasilva
 */
public class AuthorizationGroupUsersController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationGroupUsersController.class);

    @FXML
    private ComboBox<UserViewModel> availableUsersComboBox;
    @FXML
    private ObserverButton addUserButton;
    @FXML
    private TableView<UserViewModel> userTable;
    @FXML
    private TableColumn<UserViewModel, String> removeUserColumn;
    @FXML
    private TableColumn<UserViewModel, String> usernameColumn;


    private final ObjectProperty<UnitConfigType.UnitConfig> selectedGroup = new SimpleObjectProperty<>();

    private final ObjectProperty<UserViewModel> selectedUser = new SimpleObjectProperty<>();


    @FXML
    public void initialize() {
        //#####
        //Visibility
        userTable.visibleProperty().bind(selectedGroup.isNotNull());
        availableUsersComboBox.visibleProperty().bind(selectedGroup.isNotNull());
        addUserButton.visibleProperty().bind(selectedGroup.isNotNull());
        addUserButton.disableProperty().bind(selectedGroup.isNull().or(selectedUser.isNull()));
        //#####

        //#####
        //i18n
        usernameColumn.textProperty().bind(LanguageSelection.getProperty("username"));
        addUserButton.setApplyOnNewText(String::toUpperCase);
        //#####

        //#####
        //size
        userTable.widthProperty().addListener((observable, oldValue, newValue) ->
                usernameColumn.setPrefWidth(newValue.doubleValue() - removeUserColumn.getWidth() - 2)
        );
        //#####

        //#####
        //content-formatting
        removeUserColumn.setCellFactory(new ButtonTableCellFactory<>(
                (user, cellIndex) -> removeFromGroup(user),
                () -> new SVGIcon(FontAwesomeIcon.TIMES, Constants.EXTRA_SMALL_ICON, true)
        ));

        availableUsersComboBox.setConverter(new StringConverter<UserViewModel>() {
            @Override
            public String toString(UserViewModel object) {
                return object.getName();
            }

            @Override
            public UserViewModel fromString(String string) {
                throw new UnsupportedOperationException("fromString not supported for StringConverter<UserViewModel>");
            }
        });
        //#####

        selectedGroup.addListener((observable, oldValue, newValue) -> showUserTable(newValue));
        selectedUser.bind(availableUsersComboBox.valueProperty());
    }


    private void removeFromGroup(UserViewModel user) {
        UnitConfigType.UnitConfig group = selectedGroup.get();
        if (group == null || user == null) {
            return;
        }

        if (!Dialog.getConfirmation("removeUserFromGroup.confirmation", user.getName(), group.getLabel())) {
            return;
        }

        try {
            AuthorizationGroups.tryRemoveFromGroup(selectedGroup.get(), user.getId());

            String successMessage = LanguageSelection.getLocalized("removeUserFromGroup.success",
                    user.getName(), group.getLabel());
            InfoPane.info(successMessage)
                    .backgroundColor(Color.GREEN)
                    .hideAfter(Duration.seconds(5));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);

            String failureMessage = LanguageSelection.getLocalized("removeUserFromGroup.failure",
                    user.getName(), group.getLabel(), ExceptionHelper.getCauseMessage(ex));

            InfoPane.info(failureMessage)
                    .backgroundColor(Color.RED)
                    .hideAfter(Duration.seconds(5));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

    }

    private boolean askForRemoveConfirmation(UnitConfigType.UnitConfig group, UserViewModel user) {
        String message = LanguageSelection.getLocalized("removeUserFromGroup.confirmation",
                user.getName(), group.getLabel());

        Optional<ButtonType> selection = new Alert(Alert.AlertType.CONFIRMATION, message).showAndWait();

        return selection.isPresent() && selection.get().equals(ButtonType.OK);

    }

    @FXML
    private void addUser() {
        UnitConfigType.UnitConfig group = selectedGroup.get();
        UserViewModel user = selectedUser.get();
        if (group == null || user == null) {
            return;
        }

        try {
            AuthorizationGroups.tryAddToGroup(group, user.getId());
            String successMessage = LanguageSelection.getLocalized("addUserToGroup.success",
                    user.getName(), group.getLabel());
            InfoPane.info(successMessage)
                    .backgroundColor(Color.GREEN)
                    .hideAfter(Duration.seconds(5));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);

            String failureMessage = LanguageSelection.getLocalized("addUserToGroup.failure",
                    user.getName(), group.getLabel(), ExceptionHelper.getCauseMessage(ex));

            InfoPane.info(failureMessage)
                    .backgroundColor(Color.RED)
                    .hideAfter(Duration.seconds(5));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }


    public ObjectProperty<UnitConfigType.UnitConfig> selectedGroupProperty() {
        return selectedGroup;
    }

    private void showUserTable(UnitConfigType.UnitConfig unitConfig) {
        if (unitConfig == null) {
            return;
        }

        ObservableList<UserViewModel> usersInGroup = FXCollections.observableArrayList();
        ObservableList<UserViewModel> availableUsers = FXCollections.observableArrayList();


        try {
            for (final UnitConfigType.UnitConfig userUnitConfig : Registries.getUserRegistry().getUserConfigs()) {
                availableUsers.add(new UserViewModel(userUnitConfig));
            }

            availableUsers.forEach(userViewModel -> {
                if (unitConfig.getAuthorizationGroupConfig().getMemberIdList().contains(userViewModel.getId())) {
                    usersInGroup.add(userViewModel);
                }
            });

            availableUsers.removeAll(usersInGroup);

        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        userTable.setItems(usersInGroup);
        this.availableUsersComboBox.setItems(availableUsers);
    }

    public static class UserViewModel {
        private final ReadOnlyStringProperty id;
        private final ReadOnlyStringProperty name;

        public UserViewModel(UnitConfigType.UnitConfig unitConfig) {
            this.id = new SimpleStringProperty(unitConfig.getId());
            this.name = new SimpleStringProperty(unitConfig.getUserConfig().getUserName());
        }

        public String getId() {
            return id.get();
        }

        public ReadOnlyStringProperty idProperty() {
            return id;
        }

        public String getName() {
            return name.get();
        }

        public ReadOnlyStringProperty nameProperty() {
            return name;
        }
    }

}