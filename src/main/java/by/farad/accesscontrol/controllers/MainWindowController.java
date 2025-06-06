package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.services.HttpService;
import by.farad.accesscontrol.Main;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainWindowController {
    @FXML
    private AnchorPane rightPane;

    @FXML
    private void initialize() {
        Font.loadFont(getClass().getResourceAsStream("/fonts/jejugothic.ttf"), 14);

        showMainJournal();

        Platform.runLater(() -> {
            Stage stage = (Stage) rightPane.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                try {
                    logout();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    // Показать журнал
    public void showMainJournal() {
        try {
            AnchorPane journalPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/main_journal.fxml")));
            setContent(journalPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Показать список сотрудников
    public void showWorkersList() {
        try {
            AnchorPane workersPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/workers/workers_list.fxml")));
            setContent(workersPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showRoomsList() {
        try {
            AnchorPane roomsPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/rooms/rooms_list.fxml")));
            setContent(roomsPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showAccessRanges() {
        try {
            AnchorPane accessPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/access/access_ranges.fxml")));
            setContent(accessPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showAccessGroups() {
        try {
            BorderPane groupsPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/access_groups/workers_groups.fxml")));
            setContent(groupsPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showUsers() {
        try {
            BorderPane groupsPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/users/users_list.fxml")));
            setContent(groupsPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() throws IOException, InterruptedException {
        HttpService.logout();

        Stage currentStage = (Stage) rightPane.getScene().getWindow();
        currentStage.close();

        Stage stage = new Stage();
        Main.openAuthWindow(stage);

    }

    private void setContent(Pane content) {
        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);
        rightPane.getChildren().setAll(content);
    }
}