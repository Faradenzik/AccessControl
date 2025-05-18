package by.farad.accesscontrol.controllers.groups;

import by.farad.accesscontrol.models.AccessGroup;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.Setter;

public class GroupAddController {
    @FXML private TextField nameField;
    @FXML private Button saveBtn;
    @FXML private Button delBtn;
    @FXML private Button cancelBtn;
    @Setter
    private Runnable onChange;
    @Setter
    private Stage stage;

    private AccessGroup group;

    public void setGroup(AccessGroup group) {
        if (group != null) {
            this.group = group;
            fillform();
        } else {
            this.group = new AccessGroup();
            delBtn.setManaged(false);
        }
    }

    @FXML
    private void initialize() {
        saveBtn.setOnAction(event -> save());
        delBtn.setOnAction(event -> del());
        cancelBtn.setOnAction(event -> stage.close());
    }

    private void fillform() {
        nameField.setText(group.getName());
    }

    private void save() {
        group.setName(nameField.getText());
        if (group.getId() == null) {
            HttpService.addGroup(group).thenAccept(newId -> {
                if (newId != null) {
                    Platform.runLater(() -> {
                        onChange.run();
                        stage.close();
                    });
                }
            });
        } else {
            HttpService.updateGroup(group).thenAccept(newId -> {
                if (newId != null) {
                    Platform.runLater(() -> {
                        onChange.run();
                        stage.close();
                    });
                }
            });
        }
    }
    private void del() {
        HttpService.deleteGroup(group).thenAccept(success -> {
            if (success) {
                Platform.runLater(() -> {
                    onChange.run();
                    stage.close();
                });
            }
        });
    }
}
