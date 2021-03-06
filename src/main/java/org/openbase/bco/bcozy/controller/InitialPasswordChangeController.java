package org.openbase.bco.bcozy.controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import org.openbase.bco.bcozy.util.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Controller for Initial-Password-Change.
 *
 * Constructs a new, uncloseable, Stage in front of the existing.
 *
 * @author vdasilva
 */
public class InitialPasswordChangeController {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitialPasswordChangeController.class);

    @FXML
    private Pane changePassword;

    @FXML
    private PasswordChangeController changePasswordController;

    public static Pair<Stage, InitialPasswordChangeController> loadModalStage() throws IOException {
        FXMLLoader loader = new FXMLLoader(InitialPasswordChangeController.class.getClassLoader().getResource("InitialPasswordChange.fxml"));
        Pane pane = loader.load();
        InitialPasswordChangeController controller = loader.getController();

        Stage stage = controller.constructModalStage(pane);
        return new Pair<>(stage, controller);
    }

    private Stage constructModalStage(Parent parent) {
        Stage stage = new Stage(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(Event::consume);

        changePasswordController.setOnPasswordChange(success -> {
            if (success) {
                stage.close();
            }
        });

        stage.setScene(new Scene(parent));
        stage.setWidth(440.0);
        stage.setHeight(440.0);
        ThemeManager.getInstance().applyTheme(stage.getScene());
        return stage;
    }
}
