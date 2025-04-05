package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.services.HttpService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

        // Вызов метода для авторизации через HTTP
        boolean isAuthenticated = HttpService.authenticate(login, password);

        if (isAuthenticated) {
            System.out.println("Авторизация успешна!");
        }
    }
}
