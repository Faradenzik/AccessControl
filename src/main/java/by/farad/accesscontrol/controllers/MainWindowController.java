package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.services.HttpService;
import by.farad.accesscontrol.Main;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainWindowController {
    @FXML
    private Pane rightPane;

    @FXML
    private void initialize() {
        Font.loadFont(getClass().getResourceAsStream("/fonts/jejugothic.ttf"), 14);

        try {
            Pane journalPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/main_journal.fxml")));
            rightPane.getChildren().setAll(journalPane);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
            Pane journalPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/main_journal.fxml")));
            rightPane.getChildren().setAll(journalPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Показать список сотрудников
    public void showWorkersList() {
        try {
            Pane workersPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/workers_list.fxml")));
            rightPane.getChildren().setAll(workersPane);
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
}