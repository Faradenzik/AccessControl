package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.Room;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class RoomsListController implements Initializable {
    @FXML
    private ListView workersList;
    @FXML
    private TableView roomsTable;
    @FXML
    private TableColumn<Room, Integer> floor;
    @FXML
    private TableColumn<Room, String> name;
    @FXML
    private TableColumn<Room, String> deviceId;

    private final ObservableList<Room> roomsData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roomsTable.setRowFactory(tv -> {
            TableRow<Room> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && !row.isEmpty()) {
                    Room room = row.getItem();
                    loadWorkers(room);
                }
            });
            return row;
        });

        floor.setCellValueFactory(new PropertyValueFactory<>("floor"));
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        deviceId.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
        roomsTable.setItems(roomsData);

        loadRoomsData();
    }

    public void loadRoomsData() {
        HttpService.getAllRooms().thenAccept(rooms -> {
            if (rooms != null) {
                Platform.runLater(() -> {
                    roomsData.clear();
                    roomsData.addAll(rooms);
                });
            }
        });
    }

    public void loadWorkers(Room room) {

    }
}
