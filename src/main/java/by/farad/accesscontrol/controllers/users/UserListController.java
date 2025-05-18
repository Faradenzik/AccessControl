package by.farad.accesscontrol.controllers.users;

import by.farad.accesscontrol.models.MainJournal;
import by.farad.accesscontrol.models.User;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class UserListController {
    @FXML private VBox leftPanel;
    @FXML private TextField selectUsername;
    @FXML private ComboBox<String> selectRole;
    @FXML private ComboBox<String> selectWorker;
    @FXML private Button addBtn;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> username;
    @FXML private TableColumn<User, String> fio;
    @FXML private TableColumn<User, String> role;

    private final ObservableList<User> usersData = FXCollections.observableArrayList();
    private final ObservableList<User> allUsersData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        addBtn.setOnAction(event -> openUserWindow(null));
        usersTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    User user = row.getItem();
                    openUserWindow(user);
                }
            });
            return row;
        });

        username.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUsername()));
        role.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole()));
        fio.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String fullname = "";

            if (user.getWorker() != null) {
                fullname = String.format("%s %s %s", user.getWorker().getSurname(), user.getWorker().getName(), user.getWorker().getPatronymic());
            }
            return new ReadOnlyStringWrapper(fullname);
        });

        loadData();
        setupFiltering();
    }

    private void initializeFilters() {
        Set<String> workers = usersData.stream()
                .map(user -> {
                    Worker w = user.getWorker();
                    return w != null ? w.getSurname() + " " + w.getName() + " " + w.getPatronymic() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        selectWorker.setItems(FXCollections.observableArrayList(workers));

        Set<String> roles = usersData.stream()
                .map(User::getRole)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        selectRole.setItems(FXCollections.observableArrayList(roles));
    }

    private void setupFiltering() {
        selectWorker.getEditor().textProperty().addListener((obs, oldVal, newVal) -> filterTable());
        selectWorker.valueProperty().addListener((obs, oldVal, newVal) -> filterTable());

        selectUsername.textProperty().addListener((obs, oldVal, newVal) -> filterTable());

        selectRole.getEditor().textProperty().addListener((obs, oldVal, newVal) -> filterTable());
        selectRole.valueProperty().addListener((obs, oldVal, newVal) -> filterTable());
    }

    private void filterTable() {
        String usernameFilter = selectUsername.getText().toLowerCase().trim();
        String roleFilter = selectRole.getValue() != null ? selectRole.getValue().toLowerCase().trim() : "";
        String workerFilter = selectWorker.getEditor().getText().toLowerCase().trim();

        ObservableList<User> filtered = allUsersData.filtered(user -> {
            Worker worker = user.getWorker();
            String fio = "";
            if (worker != null) {
                fio = String.format("%s %s %s",
                        worker.getSurname(),
                        worker.getName(),
                        worker.getPatronymic()).toLowerCase();
            }
            boolean matchesFio = fio.contains(workerFilter);
            boolean matchesUsername = usernameFilter.isEmpty() ||
                    (user.getUsername() != null && user.getUsername().toLowerCase().contains(usernameFilter));

            boolean matchesRole = roleFilter.isEmpty() ||
                    (user.getRole() != null && user.getRole().toLowerCase().contains(roleFilter));

            return matchesUsername && matchesRole && matchesFio;
        });

        usersTable.setItems(filtered);
    }

    private void loadData() {
        HttpService.getAllUsers().thenAccept(users -> {
            if (users != null) {
                Platform.runLater(() -> {
                    usersData.clear();
                    usersData.addAll(users);
                    allUsersData.setAll(users);
                    usersTable.setItems(usersData);
                    initializeFilters();
                });
            }
        });
    }

    private void openUserWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/users/user_window.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(user != null ? "Редактирование пользователя" : "Создание пользователя");
            stage.setScene(new Scene(root));

            UserWindowController controller = loader.getController();
            controller.setStage(stage);
            controller.setOnChange(this::loadData);
            controller.setUser(user);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
