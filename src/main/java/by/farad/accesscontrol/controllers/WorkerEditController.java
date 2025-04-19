package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Setter;

import java.util.Optional;

public class WorkerEditController {

    @FXML private TextField nameField;
    @FXML private TextField surnameField;
    @FXML private TextField patronymicField;
    @FXML private ComboBox<String> sexComboBox;
    @FXML private DatePicker birthdayPicker;
    @FXML private TextField phoneField;
    @FXML private TextField positionField;
    @FXML private TextField otdelField;
    @FXML private TextField departmentField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button deleteButton;

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
            departmentField.setText(worker.getDepartment());
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
        worker.setDepartment(departmentField.getText());

        HttpService.updateWorker(worker).thenAccept(success -> {
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

    public void editAccess() {

    }

    public void saveAccess() {

    }

    public void uploadPhoto() {

    }
}