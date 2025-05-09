package by.farad.accesscontrol.controllers.workers;

import by.farad.accesscontrol.models.AccessGroup;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkerAddController {

    @FXML private ListView<String> groupsList;
    @FXML private ImageView photoView;
    @FXML private TextField nameField;
    @FXML private TextField surnameField;
    @FXML private TextField patronymicField;
    @FXML private ComboBox<String> sexComboBox;
    @FXML private DatePicker birthdayPicker;
    @FXML private TextField phoneField;
    @FXML private TextField positionField;
    @FXML private TextField otdelField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private File selectedFile = null;

    private Worker worker;

    private final Map<AccessGroup, CheckBox> checkBoxMap = new HashMap<>();
    private final List<AccessGroup> allGroups = new ArrayList<>();

    @Setter
    private Runnable refreshCallback;
    @Setter
    private Stage stage;

    public void setWorker() {
        this.worker = new Worker();
    }

    @FXML
    private void initialize() {
        sexComboBox.getItems().addAll("Мужской", "Женский");

        saveButton.setOnAction(event -> saveWorker());
        cancelButton.setOnAction(event -> stage.close());

        loadAccessGroups();
    }

    private void loadAccessGroups() {
        HttpService.getAllAccessGroups().thenAccept(groups -> {
            Platform.runLater(() -> {
                allGroups.clear();
                allGroups.addAll(groups);
                checkBoxMap.clear();
                groupsList.getItems().clear();

                for (AccessGroup group : allGroups) {
                    CheckBox checkBox = new CheckBox(group.getName());
                    checkBoxMap.put(group, checkBox);
                    groupsList.getItems().add(group.getName()); // как placeholder
                }

                groupsList.setCellFactory(param -> new ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            for (Map.Entry<AccessGroup, CheckBox> entry : checkBoxMap.entrySet()) {
                                if (entry.getKey().getName().equals(item)) {
                                    setGraphic(entry.getValue());
                                    break;
                                }
                            }
                        }
                    }
                });
            });
        });
    }

    @FXML
    private void saveWorker() {
        worker.setName(nameField.getText());
        worker.setSurname(surnameField.getText());
        worker.setPatronymic(patronymicField.getText());
        worker.setSex(sexComboBox.getValue());
        worker.setBirthday(birthdayPicker.getValue());
        worker.setPhone(phoneField.getText());
        worker.setPosition(positionField.getText());
        worker.setOtdel(otdelField.getText());

        HttpService.addWorker(worker).thenAccept(workerId -> {
            if (workerId != null) {
                // Загружаем фото
                if (selectedFile != null) {
                    HttpService.uploadWorkerPhoto(workerId, selectedFile).thenAccept(uploadResult -> {
                        if (uploadResult) {
                            System.out.println("Фото загружено");
                        }
                    });
                }

                // Сохраняем группы
                List<AccessGroup> selectedGroups = new ArrayList<>();
                for (Map.Entry<AccessGroup, CheckBox> entry : checkBoxMap.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        selectedGroups.add(entry.getKey());
                    }
                }

                HttpService.updateWorkerGroups(workerId, selectedGroups).thenAccept(groupsSaved -> {
                    Platform.runLater(() -> {
                        if (refreshCallback != null) refreshCallback.run();
                        stage.close();
                    });
                });
            }
        });
    }

    @FXML
    private void uploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите фотографию сотрудника");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения JPG", "*.jpg")
        );

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try (InputStream stream = new FileInputStream(selectedFile)) {
                Image image = new Image(stream);
                photoView.setImage(image);
                this.selectedFile = selectedFile;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}