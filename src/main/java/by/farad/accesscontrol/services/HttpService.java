package by.farad.accesscontrol.services;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpService {

    private static final String BASE_URL = "http://localhost:8080"; // Адрес сервера

    public static boolean authenticate(String login, String password) {
        try {
            URL url = new URL(BASE_URL + "/login");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Создание JSON
            String jsonInputString = "{\"login\": \"" + login + "\", \"password\": \"" + password + "\"}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Получение кода ответа
            int responseCode = connection.getResponseCode();
            InputStream is;

            // Если код ошибки не 200 - чтение данных из потока ошибок
            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                is = connection.getErrorStream();
            } else {
                is = connection.getInputStream();
            }

            // Чтение ответа от сервера
            String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                if (response.contains("Успешная авторизация")) {
                    return true;
                }
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                showAlert("Ошибка", "Неверный логин или пароль");
                return false;
            } else {
                showAlert("Ошибка", "Ошибка на сервере: " + response);
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка подключения", "Не удалось подключиться к серверу.");
            return false;
        }
        return false;
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}