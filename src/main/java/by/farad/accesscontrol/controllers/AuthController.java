package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class AuthController {
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button authButton;

    @FXML
    private void initialize() {
        authButton.setOnAction(event -> handleAuth());
    }

    private void handleAuth() {
        String login = loginField.getText();
        String password = passwordField.getText();

        HttpService.authenticateAsync(login, password)
                .thenAccept(username -> {
                    if (username != null) {
                        Platform.runLater(() -> openMainWindow(username));
                    }
                });
    }

    private void openMainWindow(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/main_window.fxml"));
            Parent root = loader.load();

            MainWindowController mainController = loader.getController();
            mainController.setCurrentUser(username);

            Stage stage = new Stage();
            stage.setTitle("Access Control System: " + username);
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            Stage currentStage = (Stage) authButton.getScene().getWindow();
            currentStage.close();

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
