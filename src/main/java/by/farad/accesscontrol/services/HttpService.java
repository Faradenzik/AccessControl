package by.farad.accesscontrol.services;

import by.farad.accesscontrol.models.Room;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.models.WorkerRoomPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
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

    private HttpService() {

    }

    // Метод для авторизации, сохранение токена
    public static CompletableFuture<String> authenticateAsync(String login, String password) {
        String json = String.format("{\"login\": \"%s\", \"password\": \"%s\"}", login, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            authToken = response.body();
                            return login;
                        } catch (Exception e) {
                            showAlert("Ошибка", "Ошибка при получении токена.");
                        }
                    }
                    return null;
                });
    }

    public static void logout() {
        if (authToken != null) {
            String json = "{}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/logout"))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)  // Передаем токен для выхода
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            try {
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                authToken = null;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Получение списка работников
    public static CompletableFuture<List<Worker>> getAllWorkers() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/workers"))
                .header("Content-Type", "application/json")
                .header("X-Session-Token", authToken)  // Передаем токен для авторизации
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

    // Обновление работника
    public static CompletableFuture<Boolean> updateWorker(Worker worker) {
        try {
            String json = objectMapper.writeValueAsString(worker);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/workers/" + worker.getId()))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)  // Передаем токен
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null)
                            return false;

                        if (response.statusCode() == 200) {
                            return true;
                        } else {
                            showAlert("Ошибка", "Не удалось обновить сотрудника. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return false;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    // Удаление работника
    public static CompletableFuture<Boolean> deleteWorker(Long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/workers/" + id))
                .header("X-Session-Token", authToken)
                .DELETE()
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response == null)
                        return false;

                    if (response.statusCode() == 200) {
                        return true;
                    } else {
                        showAlert("Ошибка", "Не удалось удалить сотрудника. Код ответа: " +
                                response.statusCode() + "\n" + response.body());
                        return false;
                    }
                });
    }

    public static CompletableFuture<List<Room>> getAllRooms() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/rooms"))
                .header("Content-Type", "application/json")
                .header("X-Session-Token", authToken)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(
                                    response.body(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, Room.class)
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

    public static CompletableFuture<List<WorkerRoomPair>> getAccessibleRooms(Long worker_id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/accesses/" + worker_id + "/rooms"))
                .header("Content-Type", "application/json")
                .header("X-Session-Token", authToken)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(
                                    response.body(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, WorkerRoomPair.class)
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

    public static CompletableFuture<Boolean> setRoomsToWorker(Set<Long> rooms, Long worker_id) {
        try {
            String json = objectMapper.writeValueAsString(rooms);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/accesses/" + worker_id))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)  // Передаем токен
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null)
                            return false;

                        if (response.statusCode() == 200) {
                            return true;
                        } else {
                            showAlert("Ошибка", "Не удалось обновить доступные помещения. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return false;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    // Метод для отправки асинхронных запросов
    private static CompletableFuture<HttpResponse<String>> sendRequestAsync(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .exceptionally(ex -> {
                    showAlert("Ошибка подключения", "Не удалось подключиться к серверу");
                    return null;
                });
    }

    // Отображение сообщений об ошибках
    private static void showAlert(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}