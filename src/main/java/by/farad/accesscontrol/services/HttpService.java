package by.farad.accesscontrol.services;

import by.farad.accesscontrol.models.Worker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HttpService {
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static String authToken;

    private HttpService() {}

    public static CompletableFuture<String> authenticateAsync(String login, String password) {
        String json = String.format("{\"login\": \"%s\", \"password\": \"%s\"}", login, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(15))
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        // Сохраняем базовую авторизацию для последующих запросов
                        authToken = Base64.getEncoder()
                                .encodeToString((login + ":" + password).getBytes());
                        return login;
                    } else if (response.statusCode() == 401) {
                        showAlert("Ошибка", "Неверный логин или пароль");
                    } else {
                        showAlert("Ошибка", "Ошибка на сервере: " + response.body());
                    }
                    return null;
                });
    }

    public static CompletableFuture<List<Worker>> getWorkersAsync() {
        if (authToken == null) {
            showAlert("Ошибка", "Требуется авторизация");
            return CompletableFuture.completedFuture(null);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/workers"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + authToken)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(
                                    response.body(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, Worker.class)
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                            showAlert("Ошибка", "Ошибка при обработке данных.");
                        }
                    } else {
                        showAlert("Ошибка", "Ошибка на сервере: " + response.statusCode() + " - " + response.body());
                    }
                    return null;
                });
    }

    private static CompletableFuture<HttpResponse<String>> sendRequestAsync(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    showAlert("Ошибка подключения", "Не удалось подключиться к серверу: " + ex.getMessage());
                    return null;
                });
    }

    private static void showAlert(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}