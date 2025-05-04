package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import com.gluonhq.charm.glisten.control.ToggleButtonGroup;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

public class RangeAddController {
    @FXML private ToggleButtonGroup days;
    @FXML private ToggleButton monday;
    @FXML private ToggleButton tuesday;
    @FXML private ToggleButton wednesday;
    @FXML private ToggleButton thursday;
    @FXML private ToggleButton friday;
    @FXML private ToggleButton saturday;
    @FXML private ToggleButton sunday;
    @FXML private ComboBox roomList;
    @FXML private TextField sh;
    @FXML private TextField sm;
    @FXML private TextField eh;
    @FXML private TextField em;
    @FXML private ChoiceBox listObj;

    private final ObservableList<Worker> workersData = FXCollections.observableArrayList();

    @Setter
    private Stage stage;


    @FXML
    private void initialize() {
        loadRooms();
        loadWorkersData();
    }

    private void loadRooms() {
        HttpService.getAllRooms().thenAccept(rooms -> {
            if (rooms != null) {
                Platform.runLater(() -> {
                    List<String> roomNames = rooms.stream()
                            .map(room -> room.getName() + ", " + room.getFloor())  // Форматируем строки
                            .collect(Collectors.toList());

                    roomList.setItems(FXCollections.observableArrayList(roomNames));
                });
            }
        });
    }

    public void loadWorkersData() {
        HttpService.getAllWorkers().thenAccept(workers -> {
            if (workers != null) {
                Platform.runLater(() -> {
                    workersData.clear();
                    workersData.addAll(workers);
                });
            }
        });
    }

    @FXML
    private void saveRange () {
    }

    @FXML
    private void setWorkerBtn() {

        listObj.setItems(workersData);
    }

    @FXML
    private void setGroupBtn() {
    }
}
