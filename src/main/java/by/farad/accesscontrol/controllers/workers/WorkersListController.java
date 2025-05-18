package by.farad.accesscontrol.controllers.workers;

import by.farad.accesscontrol.models.AccessGroup;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import org.controlsfx.control.CheckComboBox;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class WorkersListController implements Initializable {
    @FXML private ComboBox<String> selectFIO;
    @FXML private ComboBox<String> selectOtdel;
    @FXML private ChoiceBox<String> selectSex;
    @FXML private ComboBox<String> selectPosition;
    @FXML private CheckComboBox<String> selectGroups;
    @FXML private TextField sAge;
    @FXML private TextField eAge;
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
    @FXML private Label passIdLbl;

    private final ObservableList<Worker> workersData = FXCollections.observableArrayList();
    private final ObservableList<Worker> allWorkersData = FXCollections.observableArrayList();

    @FXML
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

        loadWorkersData();
        setupFiltering();
    }

    private void initializeFilters() {
        Set<String> workers = workersData.stream()
                .map(worker -> worker.getSurname() + " " + worker.getName() + " " + worker.getPatronymic())
                .collect(Collectors.toCollection(TreeSet::new));
        selectFIO.setItems(FXCollections.observableArrayList(workers));

        Set<String> otdels = workersData.stream()
                .map(Worker::getOtdel)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        selectOtdel.setItems(FXCollections.observableArrayList(otdels));

        Set<String> positions = workersData.stream()
                .map(Worker::getPosition)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        selectPosition.setItems(FXCollections.observableArrayList(positions));

        Set<String> allGroups = workersData.stream()
                .filter(w -> w.getGroups() != null)
                .flatMap(w -> w.getGroups().stream())
                .filter(Objects::nonNull)
                .map(AccessGroup::getName)
                .collect(Collectors.toCollection(TreeSet::new));
        ObservableList<String> groupItems = FXCollections.observableArrayList();
        groupItems.add("Все");
        groupItems.addAll(allGroups);
        selectGroups.getItems().setAll(groupItems);
        selectGroups.getCheckModel().check("Все");
    }

    private void setupFiltering() {
        selectFIO.getEditor().textProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        selectFIO.valueProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        selectSex.setItems(FXCollections.observableArrayList("Все", "Мужской", "Женский"));
        selectSex.setValue("Все");
        selectSex.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        sAge.textProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        eAge.textProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        selectOtdel.getEditor().textProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        selectOtdel.valueProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        selectPosition.getEditor().textProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        selectPosition.valueProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        selectGroups.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> filterJournal());
    }

    private void filterJournal() {
        String workerFilter = selectFIO.getEditor().getText().toLowerCase();
        String selectedSex = selectSex.getValue();
        String otdelFilter = selectOtdel.getEditor().getText().toLowerCase();
        String positionFilter = selectPosition.getEditor().getText().toLowerCase();

        int tmpMinAge = 0;
        int tmpMaxAge = 200;

        try {
            if (!sAge.getText().isBlank()) {
                tmpMinAge = Integer.parseInt(sAge.getText());
            }
        } catch (NumberFormatException ignored) {}

        try {
            if (!eAge.getText().isBlank()) {
                tmpMaxAge = Integer.parseInt(eAge.getText());
            }
        } catch (NumberFormatException ignored) {}

        final int minAge = tmpMinAge;
        final int maxAge = tmpMaxAge;


        List<String> selectedGroups = selectGroups.getCheckModel().getCheckedItems();

        List<TableColumn<Worker, ?>> sortOrder = new ArrayList<>(workersTable.getSortOrder());

        List<Worker> filtered = allWorkersData.filtered(worker -> {
            String fio;
            String otdel;
            String position;
            fio = String.format("%s %s %s",
                    worker.getSurname(),
                    worker.getName(),
                    worker.getPatronymic()).toLowerCase();
            boolean matchesFio = fio.contains(workerFilter);

            String status = worker.getSex() != null ? worker.getSex() : "";
            boolean matchesSex = selectedSex.equals("Все") || status.equalsIgnoreCase(selectedSex);

            int age = 0;
            if (worker.getBirthday() != null) {
                age = LocalDate.now().getYear() - worker.getBirthday().getYear();
                if (LocalDate.now().getDayOfYear() < worker.getBirthday().getDayOfYear()) {
                    age--;
                }
            }

            boolean matchesAge = age >= minAge && age <= maxAge;


            otdel = worker.getOtdel() != null ? worker.getOtdel().toLowerCase() : "";
            boolean matchesOtdel = otdel.contains(otdelFilter);

            position = worker.getPosition() != null ? worker.getPosition().toLowerCase() : "";
            boolean matchesPosition = position.contains(positionFilter);

            boolean matchesGroups = selectedGroups.contains("Все") || selectedGroups.isEmpty() ||
                    worker.getGroups().stream()
                            .map(AccessGroup::getName)
                            .anyMatch(selectedGroups::contains);

            return matchesFio && matchesOtdel && matchesPosition &&
                    matchesSex && matchesGroups && matchesAge;
        });

        workersData.setAll(filtered);
        workersTable.getSortOrder().setAll(sortOrder);
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
        if (worker.getGroups() != null) {
            for (AccessGroup group : worker.getGroups()) {
                groupNames.add(group.getName());
            }
        }
        groupsList.setItems(groupNames);

        passIdLbl.setText(worker.getPassId());
    }

    private void openEditWindow(Worker worker) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/workers/worker_edit.fxml"));
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
                    allWorkersData.setAll(workers);
                    workersTable.setItems(workersData);
                    initializeFilters();
                    filterJournal();
                });
            }
        });
    }

    @FXML
    public void addWorker() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/workers/worker_add.fxml"));
            Parent root = loader.load();

            WorkerAddController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Создание сотрудника");
            stage.setScene(new Scene(root));
            controller.setStage(stage);

            controller.setOnChangeCallback(this::loadWorkersData);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}