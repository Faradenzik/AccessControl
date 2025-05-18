package by.farad.accesscontrol.controllers.users;

import by.farad.accesscontrol.models.User;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UserWindowController {
    @FXML private TextField usernameField;
    @FXML private TextField roleField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Worker> workerField;
    @FXML private Button cancelBtn;
    @FXML private Button delBtn;
    @FXML private Button saveBtn;
    @Setter
    private Runnable onChange;
    @Setter
    private Stage stage;

    private User user;

    public void setUser(User user) {
        if (user != null) {
            this.user = user;
        } else {
            this.user = new User();
            delBtn.setManaged(false);
        }
    }

    @FXML
    private void initialize() {
        saveBtn.setOnAction(event -> save());
        delBtn.setOnAction(event -> del());
        cancelBtn.setOnAction(event -> stage.close());

        HttpService.getAllWorkers().thenAccept(workers -> {
            if (workers != null) {
                Platform.runLater(() -> {
                    workerField.setConverter(new StringConverter<>() {
                        @Override
                        public String toString(Worker worker) {
                            if (worker == null) return "";
                            return String.format("(%s) %s %s %s", worker.getId(), worker.getSurname(), worker.getName(), worker.getPatronymic());
                        }

                        @Override
                        public Worker fromString(String string) {
                            return workerField.getItems().stream()
                                    .filter(w -> w != null && String.format("(%s) %s %s %s", w.getId(), w.getSurname(), w.getName(), w.getPatronymic()).equals(string))
                                    .findFirst().orElse(null);
                        }
                    });

                    ObservableList<Worker> workerList = FXCollections.observableArrayList();
                    workerList.add(null);
                    workerList.addAll(workers);

                    workerField.setItems(workerList);
                    fillform();
                });
            }
        });
    }

    private void fillform() {
        usernameField.setText(user.getUsername());
        roleField.setText(user.getRole());
        passwordField.setText(user.getPassword());
        workerField.setValue(user.getWorker() != null ? user.getWorker() : null);
    }

    private void save() {
        user.setUsername(usernameField.getText());
        user.setRole(roleField.getText());
        user.setPassword(passwordField.getText());
        user.setWorker(workerField.getValue());

        if (user.getId() == null) {
            HttpService.addUser(user).thenAccept(statusCode -> {
                if (statusCode == 200 || statusCode == 201) {
                    Platform.runLater(() -> {
                        onChange.run();
                        stage.close();
                    });
                }
            });
        } else {
            HttpService.updateUser(user).thenAccept(statusCode -> {
                if (statusCode == 200 || statusCode == 201) {
                    Platform.runLater(() -> {
                        onChange.run();
                        stage.close();
                    });
                }
            });
        }
    }

    private void del() {
        HttpService.deleteUser(user).thenAccept(statusCode -> {
            if (statusCode == 200 || statusCode == 201) {
                Platform.runLater(() -> {
                    onChange.run();
                    stage.close();
                });
            }
        });
    }
}
