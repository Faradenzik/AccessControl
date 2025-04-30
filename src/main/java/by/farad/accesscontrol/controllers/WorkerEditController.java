package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.AccessGroup;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.*;

public class WorkerEditController {

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
    @FXML private Button saveAccessBtn;
    @FXML private Button cancelButton;
    @FXML private Button deleteButton;
    @FXML private ListView<String> groupsList;

    private List<AccessGroup> allGroups = new ArrayList<>();
    private final Map<AccessGroup, CheckBox> checkBoxMap = new HashMap<>();
    private File selectedFile = null;

    private Worker worker;
    @Setter
    private Runnable refreshCallback;
    @Setter
    private Stage stage;

    public void setWorker(Worker worker) {
        this.worker = worker;
        fillForm();
    }

    @FXML
    private void initialize() {
        sexComboBox.getItems().addAll("Мужской", "Женский");

        saveButton.setOnAction(event -> saveWorker());
        cancelButton.setOnAction(event -> stage.close());
        deleteButton.setOnAction(event -> deleteWorker());
    }

    @FXML
    private void editAccess() {
        saveAccessBtn.setVisible(true);
        HttpService.getAllAccessGroups().thenAccept(groups -> {
            Platform.runLater(() -> {
                allGroups.clear();
                allGroups.addAll(groups);
                checkBoxMap.clear();

                groupsList.getItems().clear();

                for (AccessGroup group : allGroups) {
                    CheckBox checkBox = new CheckBox(group.getName());

                    // Проставляем флажок, если работник уже в этой группе
                    boolean isSelected = worker.getGroups().stream()
                            .anyMatch(g -> g.getId() == group.getId());
                    checkBox.setSelected(isSelected);

                    checkBoxMap.put(group, checkBox);
                    groupsList.getItems().add(group.getName()); // placeholder
                }

                // Переопределяем отображение элементов на чекбоксы
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
    private void saveAccess() {
        saveAccessBtn.setVisible(false);
        List<AccessGroup> selectedGroups = new ArrayList<>();
        for (Map.Entry<AccessGroup, CheckBox> entry : checkBoxMap.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedGroups.add(entry.getKey());
            }
        }
        worker.setGroups(selectedGroups);

        // Возвращаем отображение в обычный список
        ObservableList<String> groupNames = FXCollections.observableArrayList();
        for (AccessGroup group : selectedGroups) {
            groupNames.add(group.getName());
        }

        groupsList.setCellFactory(null); // сбрасываем фабрику ячеек
        groupsList.setItems(groupNames);
    }

    private void fillForm() {
        if (worker != null) {
            nameField.setText(worker.getName());
            surnameField.setText(worker.getSurname());
            patronymicField.setText(worker.getPatronymic());
            sexComboBox.setValue(worker.getSex());
            birthdayPicker.setValue(worker.getBirthday());
            phoneField.setText(worker.getPhone());
            positionField.setText(worker.getPosition());
            otdelField.setText(worker.getOtdel());
            photoView.setImage(worker.getPhoto());
            ObservableList<String> groupNames = FXCollections.observableArrayList();
            for (AccessGroup group : worker.getGroups()) {
                groupNames.add(group.getName());
            }
            groupsList.setItems(groupNames);
        }
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

        HttpService.updateWorker(worker).thenAccept(success -> {
            if (success) {
                // 2. Загружаем фото, если выбрано
                if (selectedFile != null) {
                    HttpService.uploadWorkerPhoto(worker.getId(), selectedFile).thenAccept(uploadResult -> {
                        if (uploadResult) {
                            System.out.println("Фото загружено");
                        }
                    });
                }

                // 3. Отправляем список групп на сервер
                HttpService.updateWorkerGroups(worker.getId(), worker.getGroups()).thenAccept(groupUpdateSuccess -> {
                    if (groupUpdateSuccess) {
                        Platform.runLater(() -> {
                            if (refreshCallback != null) {
                                refreshCallback.run();
                            }
                            stage.close();
                        });
                    }
                });
            }
        });
    }

    @FXML
    private void deleteWorker() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Вы уверены, что хотите удалить сотрудника?");
        alert.setContentText(worker.getSurname() + " " + worker.getName() + " " + worker.getPatronymic());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            HttpService.deleteWorker(worker.getId()).thenAccept(success -> {
                if (success) {
                    Platform.runLater(() -> {
                        if (refreshCallback != null) {
                            refreshCallback.run();
                        }
                        stage.close();
                    });
                }
            });
        }
    }


    public void uploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите фотографию сотрудника");

        // Устанавливаем фильтр для файлов (только JPG/JPEG)
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения JPG", "*.jpg")
        );

        // Показываем диалог выбора файла
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Загружаем изображение из файла
                InputStream stream = new FileInputStream(selectedFile);
                Image image = new Image(stream);

                photoView.setImage(image);

                this.selectedFile = selectedFile;

                stream.close();

            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText("Не удалось загрузить фотографию");
                alert.setContentText(e.getMessage());
                alert.showAndWait();

                e.printStackTrace();
            }
        }
    }
}