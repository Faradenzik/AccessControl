package by.farad.accesscontrol.controllers.rooms;

import by.farad.accesscontrol.models.Room;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Setter;

import java.util.Optional;

public class RoomEditController {
    @FXML private Button cancelButton;
    @FXML private Spinner<Integer> floorField;
    @FXML private TextField nameField;
    @FXML private TextField deviceIdField;

    private Room room;
    @Setter
    private Runnable refreshCallback;
    @Setter
    private Stage stage;

    public void setRoom(Room room) {
        this.room = room;
        fillform();
    }

    @FXML
    private void initialize() {
        floorField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        cancelButton.setOnAction(event -> stage.close());
    }

    private void fillform() {
        if (room != null) {
            nameField.setText(room.getName());
            deviceIdField.setText(room.getDeviceId());
            floorField.getEditor().setText(String.valueOf(room.getFloor()));
        }
    }

    @FXML
    private void saveRoom() {
        room.setName(nameField.getText());
        room.setDeviceId(deviceIdField.getText());
        if (floorField.getValue() != null) {
            room.setFloor(floorField.getValue());
        }
        HttpService.updateRoom(room).thenAccept(roomId -> {
            if (roomId != null) {
                Platform.runLater(() -> {
                    if (refreshCallback != null) refreshCallback.run();
                    stage.close();
                });
            }
        });
    }

    @FXML
    private void deleteRoom() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Вы уверены, что хотите удалить помещение?");
        alert.setContentText(room.getName() + " | " + room.getFloor());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            HttpService.deleteRoom(room.getId()).thenAccept(success -> {
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
}
