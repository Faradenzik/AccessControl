package by.farad.accesscontrol.services;

import by.farad.accesscontrol.models.Worker;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HttpService {

    public static class ObjectMapperSingleton {
        private static final ObjectMapper objectMapper = new ObjectMapper();

        private ObjectMapperSingleton() {}

        public static ObjectMapper getObjectMapper() {
            return objectMapper;
        }
    }

    private static final String BASE_URL = "http://localhost:8080"; // Адрес сервера

    public static String authenticate(String login, String password) {
        try {
            URL url = new URL(BASE_URL + "/login");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = "{\"login\": \"" + login + "\", \"password\": \"" + password + "\"}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            InputStream is = (responseCode < 400) ? connection.getInputStream() : connection.getErrorStream();
            String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return login;
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                showAlert("Ошибка", "Неверный логин или пароль");
            } else {
                showAlert("Ошибка", "Ошибка на сервере: " + response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка подключения", "Не удалось подключиться к серверу.");
        }
        return null;
    }

    public static List<Worker> getWorkers() {
        try {
            URL url = new URL(BASE_URL + "/workers");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            InputStream is = (responseCode < 400) ? connection.getInputStream() : connection.getErrorStream();
            String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                ObjectMapper objectMapper = ObjectMapperSingleton.getObjectMapper();
                return objectMapper.readValue(response, objectMapper.getTypeFactory().constructCollectionType(List.class, Worker.class));
            } else {
                showAlert("Ошибка", "Ошибка на сервере: " + response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка подключения", "Не удалось подключиться к серверу.");
        }
        return null;
    }


    private static void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}