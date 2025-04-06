package by.farad.accesscontrol.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;

import java.io.IOException;
import java.util.Objects;

public class MainWindowController {

    public Button workers_list_btn;

    public Button main_journal_btn;

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
    }


    public void showMainJournal() {
        try {
            Pane journalPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/main_journal.fxml")));
            rightPane.getChildren().setAll(journalPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showWorkersList() {
        try {
            Pane workersPane = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/by/farad/accesscontrol/workers_list.fxml")));
            rightPane.getChildren().setAll(workersPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
