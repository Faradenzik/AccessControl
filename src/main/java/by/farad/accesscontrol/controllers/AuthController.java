package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.Session;
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
                        Platform.runLater(() -> {
                            Session.setCurrentUsername(username);
                            openMainWindow();
                        });
                    }
                });
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/main_window.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Главное окно");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();

            // Закрыть окно авторизации
            Stage currentStage = (Stage) authButton.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
