package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class WorkersListController {

    @FXML
    private TableView<Worker> workersTable;

    @FXML
    private TableColumn<Worker, Long> id;

    @FXML
    private TableColumn<Worker, String> name;

    @FXML
    private TableColumn<Worker, String> surname;

    @FXML
    private TableColumn<Worker, String> patronomyc;

    @FXML
    private TableColumn<Worker, String> sex;

    @FXML
    private TableColumn<Worker, String> birthday;

    @FXML
    private TableColumn<Worker, String> position;

    @FXML
    private TableColumn<Worker, String> department;

    @FXML
    public void initialize() {
        id.setCellValueFactory(new PropertyValueFactory<>("id"));
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        surname.setCellValueFactory(new PropertyValueFactory<>("surname"));
        patronomyc.setCellValueFactory(new PropertyValueFactory<>("patronomyc"));
        sex.setCellValueFactory(new PropertyValueFactory<>("sex"));
        birthday.setCellValueFactory(new PropertyValueFactory<>("birthday"));
        position.setCellValueFactory(new PropertyValueFactory<>("position"));
        department.setCellValueFactory(new PropertyValueFactory<>("department"));

        List<Worker> workers = HttpService.getWorkers();
        if (workers != null) {
            ObservableList<Worker> workersList = FXCollections.observableArrayList(workers);
            workersTable.setItems(workersList);
        }
    }
}
