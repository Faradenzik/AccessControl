package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.Room;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WorkerAddController {

    @FXML private ImageView photoView;
    @FXML private TreeView<String> availableRoomsTree;
    @FXML private TextField nameField;
    @FXML private TextField surnameField;
    @FXML private TextField patronymicField;
    @FXML private ComboBox<String> sexComboBox;
    @FXML private DatePicker birthdayPicker;
    @FXML private TextField phoneField;
    @FXML private TextField positionField;
    @FXML private TextField otdelField;
    @FXML private TextField departmentField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private File selectedFile = null;
    private CompletableFuture<List<Room>> allRoomsFuture;
    private Set<Long> accessibleRoomIds = new HashSet<>();
    private Set<Long> currentlySelectedRoomIds = new HashSet<>();

    private Worker worker;
    @Setter
    private Runnable refreshCallback;
    @Setter
    private Stage stage;

    public void setWorker() {
        this.worker = new Worker();
        initializeRoomTree();
    }

    @FXML
    private void initialize() {
        sexComboBox.getItems().addAll("Мужской", "Женский");

        saveButton.setOnAction(event -> saveWorker());
        cancelButton.setOnAction(event -> stage.close());
    }

    public void initializeRoomTree() {
        allRoomsFuture = HttpService.getAllRooms();
        allRoomsFuture.thenAccept(allRooms -> {
            Platform.runLater(() -> {
                buildRoomTree(allRooms);  // включаем режим редактирования
            });
        });
    }

    private void buildRoomTree(List<Room> allRooms) {
        // Фильтрация комнат
        List<Room> roomsToShow = allRooms.stream()
                .filter(room -> true || accessibleRoomIds.contains(room.getId()))
                .toList();

        // Создаем корневой элемент
        TreeItem<String> root = new TreeItem<>("Доступные помещения");
        root.setExpanded(true);

        // Группируем по этажам
        Map<Integer, List<Room>> roomsByFloor = roomsToShow.stream()
                .collect(Collectors.groupingBy(Room::getFloor));

        // Заполняем дерево
        roomsByFloor.forEach((floor, rooms) -> {
            // Элемент этажа
            TreeItem<String> floorItem = new TreeItem<>(floor + " этаж");
            floorItem.setExpanded(true);

            // Добавляем комнаты
            rooms.forEach(room -> {
                TreeItem<String> roomItem = new TreeItem<>(room.getName());

                if (true) {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(currentlySelectedRoomIds.contains(room.getId()));
                    checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal) {
                            currentlySelectedRoomIds.add(room.getId());
                        } else {
                            currentlySelectedRoomIds.remove(room.getId());
                        }
                    });

                    // Создаем графический элемент с CheckBox
                    roomItem.setGraphic(new HBox(5, checkBox, new Label(room.getName())));
                }

                floorItem.getChildren().add(roomItem);
            });

            root.getChildren().add(floorItem);
        });

        Platform.runLater(() -> {
            availableRoomsTree.setRoot(root);
            availableRoomsTree.setShowRoot(true);

            // Устанавливаем кастомный CellFactory для правильного отображения
            availableRoomsTree.setCellFactory(tv -> new TreeCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        TreeItem<String> treeItem = getTreeItem();
                        if (treeItem != null) {
                            // Для элементов с CheckBox используем графическое представление
                            if (treeItem.getGraphic() != null) {
                                setText(null);
                                setGraphic(treeItem.getGraphic());
                            } else {
                                setText(item);
                                setGraphic(null);
                            }
                        }
                    }
                }
            });
        });
    }

    @FXML
    private void saveWorker() {
        worker.setName(nameField.getText());
        worker.setSurname(surnameField.getText());
        worker.setPatronymic(patronymicField.getText());
        worker.setSex(sexComboBox.getValue());
        worker.setBirthday(birthdayPicker.getValue());
        worker.setPhone(phoneField.getText());
        worker.setPosition(positionField.getText());
        worker.setOtdel(otdelField.getText());
        worker.setDepartment(departmentField.getText());
        accessibleRoomIds = new HashSet<>(currentlySelectedRoomIds);

        HttpService.addWorker(worker).thenAccept(workerId -> {
            if (workerId != null) {
                Platform.runLater(() -> {
                    if (refreshCallback != null) {
                        refreshCallback.run();
                    }
                    stage.close();
                    if (!accessibleRoomIds.isEmpty()) {
                        HttpService.setRoomsToWorker(accessibleRoomIds, workerId);
                    }
                    if (selectedFile != null) {
                        HttpService.uploadWorkerPhoto(workerId, selectedFile).thenAccept(uploadResult -> {
                            if (uploadResult) {
                                System.out.println("загружено");
                            }
                        });
                    }
                });
            }
        });
    }

    public void uploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите фотографию сотрудника");

        // Устанавливаем фильтр для файлов (только JPG/JPEG)
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения JPG", "*.jpg")
        );

        // Показываем диалог выбора файла
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Загружаем изображение из файла
                InputStream stream = new FileInputStream(selectedFile);
                Image image = new Image(stream);

                photoView.setImage(image);

                this.selectedFile = selectedFile;

                stream.close();

            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText("Не удалось загрузить фотографию");
                alert.setContentText(e.getMessage());
                alert.showAndWait();

                e.printStackTrace();
            }
        }
    }
}