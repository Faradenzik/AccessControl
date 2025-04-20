package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class WorkersListController implements Initializable {

    @FXML
    public TreeView<String> treeView;
    @FXML
    public TableColumn<Worker, String> phone;

    @FXML
    public TableColumn<Worker, String> otdel;

    @FXML
    private TableView<Worker> workersTable;

    @FXML
    private TableColumn<Worker, Number> id;

    @FXML
    private TableColumn<Worker, String> name;

    @FXML
    private TableColumn<Worker, String> surname;

    @FXML
    private TableColumn<Worker, String> patronymic;

    @FXML
    private TableColumn<Worker, String> sex;

    @FXML
    private TableColumn<Worker, LocalDate> birthday;

    @FXML
    private TableColumn<Worker, String> position;

    @FXML
    private TableColumn<Worker, String> department;

    private final ObservableList<Worker> workersData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        workersTable.setRowFactory(tv -> {
            TableRow<Worker> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Worker worker = row.getItem();
                    openEditWindow(worker);
                }
            });
            return row;
        });

        id.setCellValueFactory(new PropertyValueFactory<>("id"));
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        surname.setCellValueFactory(new PropertyValueFactory<>("surname"));
        patronymic.setCellValueFactory(new PropertyValueFactory<>("patronymic"));
        sex.setCellValueFactory(new PropertyValueFactory<>("sex"));
        birthday.setCellValueFactory(new PropertyValueFactory<>("birthday"));
        phone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        position.setCellValueFactory(new PropertyValueFactory<>("position"));
        otdel.setCellValueFactory(new PropertyValueFactory<>("otdel"));
        department.setCellValueFactory(new PropertyValueFactory<>("department"));

        workersTable.setItems(workersData);

        loadWorkersData();
        setupTreeViewListener();
    }

    private void openEditWindow(Worker worker) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/worker_edit.fxml"));
            Parent root = loader.load();

            WorkerEditController controller = loader.getController();
            controller.setWorker(worker);

            Stage stage = new Stage();
            stage.setTitle("Редактирование сотрудника");
            stage.setScene(new Scene(root));
            controller.setStage(stage);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadWorkersData() {
        HttpService.getAllWorkers().thenAccept(workers -> {
            if (workers != null) {
                Platform.runLater(() -> {
                    workersData.clear();
                    workersData.addAll(workers);
                    buildTreeFromWorkers(workers); // строим дерево
                });
            }
        });
    }

    private void buildTreeFromWorkers(List<Worker> workers) {
        Map<String, Set<String>> departmentMap = new HashMap<>();

        for (Worker worker : workers) {
            String dep = worker.getDepartment();
            String otdel = worker.getOtdel();

            departmentMap.putIfAbsent(dep, new HashSet<>());
            if (otdel != null && !otdel.isBlank()) {
                departmentMap.get(dep).add(otdel);
            }
        }

        TreeItem<String> rootItem = new TreeItem<>("Все сотрудники");
        rootItem.setExpanded(true);

        for (Map.Entry<String, Set<String>> entry : departmentMap.entrySet()) {
            TreeItem<String> depItem = new TreeItem<>(entry.getKey());
            for (String otdel : entry.getValue()) {
                TreeItem<String> otdelItem = new TreeItem<>(otdel);
                depItem.getChildren().add(otdelItem);
            }
            rootItem.getChildren().add(depItem);
        }

        treeView.setRoot(rootItem);
    }

    private void setupTreeViewListener() {
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String selected = newVal.getValue();
                TreeItem<String> parent = newVal.getParent();

                if (parent == null) {
                    workersTable.setItems(workersData);
                } else if (parent.getValue().equals("Все сотрудники")) {
                    filterTableByDepartment(selected);
                } else {
                    filterTableByDepartmentAndOtdel(parent.getValue(), selected);
                }
            }
        });
    }

    private void filterTableByDepartment(String departmentName) {
        ObservableList<Worker> filtered = workersData.filtered(w -> w.getDepartment().equals(departmentName));
        workersTable.setItems(filtered);
    }

    private void filterTableByDepartmentAndOtdel(String department, String otdel) {
        ObservableList<Worker> filtered = workersData.filtered(w ->
                w.getDepartment().equals(department) && w.getOtdel().equals(otdel));
        workersTable.setItems(filtered);
    }
}