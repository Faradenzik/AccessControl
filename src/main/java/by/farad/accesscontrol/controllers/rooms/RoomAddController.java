package by.farad.accesscontrol.controllers.rooms;

import by.farad.accesscontrol.models.Room;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Setter;

public class RoomAddController {
    public Button cancelButton;
    @FXML private Spinner<Integer> floorField;
    @FXML private TextField nameField;
    @FXML private TextField deviceIdField;

    private Room room = new Room();
    @Setter
    private Runnable refreshCallback;
    @Setter
    private Stage stage;

    @FXML
    private void initialize() {
        floorField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        cancelButton.setOnAction(event -> stage.close());
    }

    @FXML
    public void saveRoom() {
        room.setName(nameField.getText());
        room.setDeviceId(deviceIdField.getText());
        if (floorField.getValue() != null) {
            room.setFloor(floorField.getValue());
        }
        HttpService.addRoom(room).thenAccept(roomId -> {
            if (roomId != null) {
                Platform.runLater(() -> {
                    if (refreshCallback != null) refreshCallback.run();
                    stage.close();
                });
            }
        });
    }
}
