package by.farad.accesscontrol.controllers.groups;

import by.farad.accesscontrol.models.AccessGroup;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import java.util.Objects;
import java.util.ResourceBundle;

public class WorkerGroupsController implements Initializable {
    @FXML private Button moveRightBtn;
    @FXML private Button moveLeftBtn;
    @FXML private Button addGroupBtn;
    @FXML private ListView<AccessGroup> groupsList;
    @FXML private TableView<Worker> workersTable;
    @FXML private TableColumn<Worker, Long> id;
    @FXML private TableColumn<Worker, String> fio;
    @FXML private TableColumn<Worker, String> position;
    @FXML private TableColumn<Worker, String> otdel;
    @FXML private TableView<Worker> workersTable1;
    @FXML private TableColumn<Worker, Long> id1;
    @FXML private TableColumn<Worker, String> fio1;
    @FXML private TableColumn<Worker, String> position1;
    @FXML private TableColumn<Worker, String> otdel1;

    private final ObservableList<AccessGroup> groupsData = FXCollections.observableArrayList();
    private final ObservableList<Worker> workersData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();

        addGroupBtn.setOnAction(event -> addGroup(null));

        groupsList.setCellFactory(listView -> {
            ListCell<AccessGroup> cell = new ListCell<>() {
                @Override
                protected void updateItem(AccessGroup item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            };

            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    addGroup(cell.getItem());
                }
            });

            return cell;
        });

        groupsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedGroup) -> updateTables());

        workersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            moveLeftBtn.setDisable(selected == null);
        });
        workersTable1.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            moveRightBtn.setDisable(selected == null);
        });

        loadData();
    }

    private void setupTableColumns() {
        id.setCellValueFactory(new PropertyValueFactory<>("id"));
        fio.setCellValueFactory(cellData -> {
            Worker worker = cellData.getValue();
            String fullName = String.format("%s %s %s", worker.getSurname(), worker.getName(), worker.getPatronymic());
            return new ReadOnlyStringWrapper(fullName);
        });
        otdel.setCellValueFactory(new PropertyValueFactory<>("otdel"));
        position.setCellValueFactory(new PropertyValueFactory<>("position"));

        id1.setCellValueFactory(new PropertyValueFactory<>("id"));
        fio1.setCellValueFactory(cellData -> {
            Worker worker = cellData.getValue();
            String fullName = String.format("%s %s %s", worker.getSurname(), worker.getName(), worker.getPatronymic());
            return new ReadOnlyStringWrapper(fullName);
        });
        otdel1.setCellValueFactory(new PropertyValueFactory<>("otdel"));
        position1.setCellValueFactory(new PropertyValueFactory<>("position"));
    }

    private void updateTables() {
        AccessGroup selectedGroup = groupsList.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            workersTable.setItems(FXCollections.emptyObservableList());
            workersTable1.setItems(FXCollections.emptyObservableList());
            moveLeftBtn.setDisable(true);
            moveRightBtn.setDisable(true);
            return;
        }

        ObservableList<Worker> groupWorkers = workersData.filtered(worker ->
                worker.getGroups() != null && worker.getGroups().stream().anyMatch(group -> group.getId().equals(selectedGroup.getId()))
        );

        ObservableList<Worker> otherWorkers = workersData.filtered(worker ->
                worker.getGroups() != null && worker.getGroups().stream().noneMatch(group -> group.getId().equals(selectedGroup.getId()))
        );

        workersTable1.setItems(groupWorkers);
        workersTable.setItems(otherWorkers);

        workersTable.getSelectionModel().clearSelection();
        workersTable1.getSelectionModel().clearSelection();
        moveLeftBtn.setDisable(true);
        moveRightBtn.setDisable(true);
    }

    public void loadData() {
        HttpService.getAllWorkers().thenAccept(workers -> {
            if (workers != null) {
                Platform.runLater(() -> {
                    workersData.clear();
                    workersData.addAll(workers);
                    updateTables();
                });
            }
        });
        HttpService.getAllAccessGroups().thenAccept(groups -> {
            if (groups != null) {
                Platform.runLater(() -> {
                    groupsData.clear();
                    groupsData.addAll(groups);
                    groupsList.setItems(groupsData);
                });
            }
        });
    }

    @FXML
    private void moveLeft() {
        Worker selectedWorker = workersTable.getSelectionModel().getSelectedItem();
        AccessGroup selectedGroup = groupsList.getSelectionModel().getSelectedItem();
        if (selectedWorker != null && selectedGroup != null) {
            selectedWorker.getGroups().add(selectedGroup);

            HttpService.updateWorkerGroups(selectedWorker.getId(), selectedWorker.getGroups())
                    .thenAccept(success -> {
                        if (success) {
                            Platform.runLater(this::updateTables);
                        }
                    });
        }
    }

    @FXML
    private void moveRight() {
        Worker selectedWorker = workersTable1.getSelectionModel().getSelectedItem();
        AccessGroup selectedGroup = groupsList.getSelectionModel().getSelectedItem();
        if (selectedWorker != null && selectedGroup != null) {
            selectedWorker.getGroups().removeIf(group -> group.getId().equals(selectedGroup.getId()));

            HttpService.updateWorkerGroups(selectedWorker.getId(), selectedWorker.getGroups())
                    .thenAccept(success -> {
                        if (success) {
                            Platform.runLater(this::updateTables);
                        }
                    });
        }
    }

    private void addGroup(AccessGroup group) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/access_groups/group_add.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(group != null ? "Редактирование группы" : "Создание группы");
            stage.setScene(new Scene(root));

            GroupAddController controller = loader.getController();
            controller.setStage(stage);
            controller.setOnChange(this::loadData);
            controller.setGroup(group);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void deleteGroup() {
        AccessGroup selected = groupsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            HttpService.deleteGroup(selected).thenAccept(success -> {
                if (success) {
                    Platform.runLater(this::loadData);
                }
            });
        }
    }
}