package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.AccessGroup;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class WorkersListController implements Initializable {
    @FXML private TreeView<String> treeView;
    @FXML private TableView<Worker> workersTable;
    @FXML private TableColumn<Worker, Long> id;
    @FXML private TableColumn<Worker, String> fio;
    @FXML private TableColumn<Worker, String> sex;
    @FXML private TableColumn<Worker, String> position;
    @FXML private TableColumn<Worker, String> groups;
    @FXML private ImageView workerPhoto;
    @FXML private VBox infoPane;
    @FXML private Label nameLbl;
    @FXML private Label surnameLbl;
    @FXML private Label patronymicLbl;
    @FXML private Label sexLbl;
    @FXML private Label birthdayLbl;
    @FXML private Label phoneLbl;
    @FXML private Label otdelLbl;
    @FXML private Label positionLbl;
    @FXML private ListView<String> groupsList;

    private final ObservableList<Worker> workersData = FXCollections.observableArrayList();
    private final FilteredList<Worker> filteredWorkers = new FilteredList<>(workersData, w -> true);
    private final SortedList<Worker> sortedWorkers = new SortedList<>(filteredWorkers);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        workersTable.setRowFactory(tv -> {
            TableRow<Worker> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Worker worker = row.getItem();
                    openEditWindow(worker);
                } else if (event.getClickCount() == 1 && !row.isEmpty()) {
                    Worker worker = row.getItem();
                    setInfoPane(worker);
                }
            });
            return row;
        });

        id.setCellValueFactory(new PropertyValueFactory<>("id"));
        fio.setCellValueFactory(cellData -> {
            Worker worker = cellData.getValue();
            String fullName = String.format("%s %s %s", worker.getSurname(), worker.getName(), worker.getPatronymic());
            return new ReadOnlyStringWrapper(fullName);
        });
        sex.setCellValueFactory(new PropertyValueFactory<>("sex"));
        position.setCellValueFactory(new PropertyValueFactory<>("position"));
        groups.setCellValueFactory(cellData -> {
            Worker worker = cellData.getValue();
            if (worker.getGroups() != null) {
                String groupNames = worker.getGroups().stream()
                        .map(AccessGroup::getName)
                        .collect(Collectors.joining(", "));
                return new ReadOnlyStringWrapper(groupNames);
            }
            return null;
        });

        sortedWorkers.comparatorProperty().bind(workersTable.comparatorProperty());
        workersTable.setItems(sortedWorkers);

        loadWorkersData();
        setupTreeViewListener();
    }

    private void setInfoPane(Worker worker) {
        infoPane.setVisible(true);
        workerPhoto.setImage(worker.getPhoto());
        nameLbl.setText(worker.getName());
        surnameLbl.setText(worker.getSurname());
        patronymicLbl.setText(worker.getPatronymic());
        sexLbl.setText(worker.getSex());
        birthdayLbl.setText(worker.getBirthday().toString());
        phoneLbl.setText(worker.getPhone());
        otdelLbl.setText(worker.getOtdel());
        positionLbl.setText(worker.getPosition());

        ObservableList<String> groupNames = FXCollections.observableArrayList();
        for (AccessGroup group : worker.getGroups()) {
            groupNames.add(group.getName());
        }
        groupsList.setItems(groupNames);
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

            stage.setOnHidden(e -> loadWorkersData());
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
                    buildTreeFromWorkers(workers); // обновляем дерево
                });
            }
        });
    }

    private void buildTreeFromWorkers(List<Worker> workers) {
        TreeItem<String> root = new TreeItem<>("Все отделы");
        root.setExpanded(true);

        Set<String> otdels = new TreeSet<>();
        for (Worker w : workers) {
            if (w.getOtdel() != null && !w.getOtdel().isBlank()) {
                otdels.add(w.getOtdel());
            }
        }

        for (String otdel : otdels) {
            root.getChildren().add(new TreeItem<>(otdel));
        }

        treeView.setRoot(root);
    }

    private void setupTreeViewListener() {
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String selected = newVal.getValue();
                if (selected.equals("Все отделы")) {
                    filteredWorkers.setPredicate(w -> true);
                } else {
                    filteredWorkers.setPredicate(w -> selected.equals(w.getOtdel()));
                }
            }
        });
    }

    @FXML
    public void addWorker() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/worker_add.fxml"));
            Parent root = loader.load();

            WorkerAddController controller = loader.getController();
            controller.setWorker();

            Stage stage = new Stage();
            stage.setTitle("Создание сотрудника");
            stage.setScene(new Scene(root));
            controller.setStage(stage);

            stage.setOnHidden(e -> loadWorkersData());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}