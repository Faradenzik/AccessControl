package by.farad.accesscontrol.services;

import by.farad.accesscontrol.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
                            // Получаем список работников без фотографий
                            List<Worker> workers = objectMapper.readValue(
                                    response.body(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, Worker.class)
                            );

                            // Загружаем фото для каждого работника
                            for (Worker worker : workers) {
                                if (worker.getPhotoFile() != null && !worker.getPhotoFile().isEmpty()) {
                                    String photoPath = worker.getPhotoFile();
                                    CompletableFuture<Image> photoFuture = getWorkerPhoto(photoPath);
                                    photoFuture.thenAccept(photo -> worker.setPhoto(photo));
                                }
                                CompletableFuture<List<AccessGroup>> groupsFuture = getWorkerGroups(worker.getId());
                                groupsFuture.thenAccept(groups -> {
                                    if (groups != null) {
                                        worker.setGroups(groups);
                                    }
                                });
                            }
                            return workers;
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

    // Загрузка фото для работника
    public static CompletableFuture<Image> getWorkerPhoto(String photoPath) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/workers/photo/" + photoPath))
                .header("X-Session-Token", authToken)
                .GET()
                .timeout(Duration.ofSeconds(20)) // Таймаут для загрузки фото может быть больше
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response == null) {
                        return null;
                    }

                    if (response.statusCode() == 200) {
                        try (InputStream imageStream = response.body()) {
                            return new Image(imageStream);
                        } catch (Exception e) {
                            return null;
                        }
                    } else if (response.statusCode() == 404) {
                        System.out.println("Фото для работника не найдено");
                        return null;
                    } else {
                        showAlert("Ошибка сервера", "Не удалось загрузить фото сотрудника. Код ответа: " +
                                response.statusCode());
                        try {
                            String errorBody = new String(response.body().readAllBytes());
                            System.err.println("Тело ошибки от сервера: " + errorBody);
                        } catch (IOException ioException) {
                            throw new RuntimeException(ioException);
                        }
                        return null;
                    }
                })
                .exceptionally(ex -> {
                    // Обработка исключений при отправке запроса (например, нет соединения)
                    System.err.println("Ошибка сети при запросе фото: " + ex.getMessage());
                    return null;
                });
    }

    // Метод для отправки фото работника на сервер
    public static CompletableFuture<Boolean> uploadWorkerPhoto(Long workerId, File file) {
        if (file == null || !file.exists()) {
            showAlert("Ошибка", "Файл не найден или не выбран.");
            return CompletableFuture.completedFuture(false);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/workers/photo/" + workerId))
                    .header("X-Session-Token", authToken)
                    .header("Content-Type", "application/octet-stream") // указываем просто бинарный поток
                    .POST(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null) return false;

                        if (response.statusCode() == 200) {
                            return true;
                        } else {
                            showAlert("Ошибка", "Не удалось загрузить фото. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return false;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
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

    public static CompletableFuture<Long> addWorker(Worker worker) {
        try {
            String json = objectMapper.writeValueAsString(worker);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/workers/"))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null) {
                            return null;
                        }

                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            try {
                                return Long.parseLong(response.body());
                            } catch (NumberFormatException e) {
                                showAlert("Ошибка", "Сервер вернул неверный ID работника.");
                                return null;
                            }
                        } else {
                            showAlert("Ошибка", "Не удалось добавить сотрудника. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return null;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

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
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(
                                    response.body(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, Room.class));
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

    public static CompletableFuture<Long> addRoom(Room room) {
        try {
            String json = objectMapper.writeValueAsString(room);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/rooms/"))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null) {
                            return null;
                        }

                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            try {
                                return Long.parseLong(response.body());
                            } catch (NumberFormatException e) {
                                showAlert("Ошибка", "Сервер вернул неверный ID помещения.");
                                return null;
                            }
                        } else {
                            showAlert("Ошибка", "Не удалось добавить помещение. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return null;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Boolean> updateRoom(Room room) {
        try {
            String json = objectMapper.writeValueAsString(room);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/rooms/" + room.getId()))
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
                            showAlert("Ошибка", "Не удалось обновить помещение. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return false;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    public static CompletableFuture<Boolean> deleteRoom(Long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/rooms/" + id))
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
                        showAlert("Ошибка", "Не удалось удалить помещение. Код ответа: " +
                                response.statusCode() + "\n" + response.body());
                        return false;
                    }
                });
    }

    public static CompletableFuture<List<AccessGroup>> getAllAccessGroups() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/access"))
                .GET()
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(
                                    response.body(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, AccessGroup.class)
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

    public static CompletableFuture<Long> addGroup(AccessGroup group) {
        try {
            String json = objectMapper.writeValueAsString(group);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/access"))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null) {
                            return null;
                        }

                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            try {
                                return Long.parseLong(response.body());
                            } catch (NumberFormatException e) {
                                showAlert("Ошибка", "Сервер вернул неверный ID группы.");
                                return null;
                            }
                        } else {
                            showAlert("Ошибка", "Не удалось добавить группу. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return null;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Boolean> updateGroup(AccessGroup group) {
        try {
            String json = objectMapper.writeValueAsString(group);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/access/" + group.getId()))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null) {
                            return null;
                        }

                        if (!(response.statusCode() == 200 || response.statusCode() == 201)) {
                            showAlert("Ошибка", "Не удалось обновить расписание. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return null;
                        }
                        return true;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Boolean> deleteGroup(AccessGroup group) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/access/" + group.getId()))
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
                        showAlert("Ошибка", "Не удалось удалить группу. Код ответа: " +
                                response.statusCode() + "\n" + response.body());
                        return false;
                    }
                });
    }

    public static CompletableFuture<List<AccessGroup>> getWorkerGroups(Long workerId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/access/worker/" + workerId + "/groups"))
                .GET()
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(
                                    response.body(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, AccessGroup.class)
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

    public static CompletableFuture<Boolean> updateWorkerGroups(Long workerId, List<AccessGroup> groups) {
        try {
            List<Long> groupIds = groups.stream().map(AccessGroup::getId).toList();
            String json = objectMapper.writeValueAsString(groupIds);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/access/worker/" + workerId + "/groups"))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null) return false;

                        if (response.statusCode() == 200) {
                            return true;
                        } else {
                            showAlert("Ошибка", "Не удалось обновить группы доступа. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return false;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    public static CompletableFuture<List<AccessTimeRange>> getAccessTimeRanges() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/access-ranges"))
                .GET()
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(
                                    response.body(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, AccessTimeRange.class)
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                            showAlert("Ошибка", "Ошибка при обработке данных расписаний.");
                        }
                    } else {
                        showAlert("Ошибка", "Ошибка сервера: " + response.statusCode() + " - " + response.body());
                    }
                    return null;
                });
    }

    public static CompletableFuture<Boolean> addAccessRange(AccessTimeRange access) {
        try {
            String json = objectMapper.writeValueAsString(access);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/access-ranges"))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null) {
                            return null;
                        }

                        if (!(response.statusCode() == 200 || response.statusCode() == 201)) {
                            showAlert("Ошибка", "Не удалось добавить расписание. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return null;
                        }
                        return true;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Boolean> updateAccessRange(AccessTimeRange access) {
        try {
            String json = objectMapper.writeValueAsString(access);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/access-ranges/" + access.getId()))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null) {
                            return null;
                        }

                        if (!(response.statusCode() == 200 || response.statusCode() == 201)) {
                            showAlert("Ошибка", "Не удалось обновить расписание. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return null;
                        }
                        return true;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Boolean> deleteRange(Long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/access-ranges/" + id))
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
                        showAlert("Ошибка", "Не удалось удалить расписание. Код ответа: " +
                                response.statusCode() + "\n" + response.body());
                        return false;
                    }
                });
    }

    public static CompletableFuture<List<MainJournal>> getMainJournal() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/main-journal"))
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
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, MainJournal.class));
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

    public static CompletableFuture<List<User>> getAllUsers() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users"))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readValue(
                                    response.body(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));
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

    public static CompletableFuture<Integer> addUser(User user) {
        try {
            String json = objectMapper.writeValueAsString(user);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/users"))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response == null) {
                            return null;
                        }

                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            try {
                                return response.statusCode();
                            } catch (NumberFormatException e) {
                                showAlert("Ошибка", "Сервер вернул неверный ID помещения.");
                                return null;
                            }
                        } else {
                            showAlert("Ошибка", "Не удалось добавить помещение. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                            return null;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Integer> updateUser(User user) {
        try {
            String json = objectMapper.writeValueAsString(user);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/users/" + user.getId()))
                    .header("Content-Type", "application/json")
                    .header("X-Session-Token", authToken)  // Передаем токен
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return sendRequestAsync(request)
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            showAlert("Ошибка", "Не удалось обновить пользователя. Код ответа: " +
                                    response.statusCode() + "\n" + response.body());
                        }
                        return response.statusCode();
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static CompletableFuture<Integer> deleteUser(User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users/" + user.getId()))
                .header("X-Session-Token", authToken)
                .DELETE()
                .build();

        return sendRequestAsync(request)
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        showAlert("Ошибка", "Не удалось удалить пользователя. Код ответа: " +
                                response.statusCode() + "\n" + response.body());
                    }
                    return response.statusCode();
                });
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