package by.farad.accesscontrol.controllers;

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

public class WorkerAddController {

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
                Platform.runLater(() -> {
                    if (refreshCallback != null) {
                        refreshCallback.run();
                    }
                    stage.close();
                    if (selectedFile != null) {
                        HttpService.uploadWorkerPhoto(workerId, selectedFile).thenAccept(uploadResult -> {
                            if (uploadResult) {
                                System.out.println("загружено");
                            }
                        });
                    }
                });
            }
        });
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