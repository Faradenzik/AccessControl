package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.Room;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.models.WorkerRoomPair;
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

public class WorkerEditController {

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
    @FXML private Button deleteButton;
    @FXML private Button saveAccessBtn;

    private File selectedFile = null;
    private CompletableFuture<List<WorkerRoomPair>> accessibleFuture;
    private CompletableFuture<List<Room>> allRoomsFuture;
    private Set<Long> accessibleRoomIds = new HashSet<>();
    private Set<Long> currentlySelectedRoomIds = new HashSet<>();

    private Worker worker;
    @Setter
    private Runnable refreshCallback;
    @Setter
    private Stage stage;

    public void setWorker(Worker worker) {
        this.worker = worker;
        fillForm();
        accessibleFuture = HttpService.getAccessibleRooms(worker.getId());
        initializeRoomTree();
    }

    @FXML
    private void initialize() {
        sexComboBox.getItems().addAll("Мужской", "Женский");

        saveButton.setOnAction(event -> saveWorker());
        cancelButton.setOnAction(event -> stage.close());
        deleteButton.setOnAction(event -> deleteWorker());
    }

    public void initializeRoomTree() {
        allRoomsFuture = HttpService.getAllRooms();

        accessibleFuture.thenCombine(allRoomsFuture, (accessibleRooms, allRooms) -> {
            accessibleRoomIds = accessibleRooms.stream()
                    .map(WorkerRoomPair::getRoom_id)
                    .collect(Collectors.toSet());
            currentlySelectedRoomIds = new HashSet<>(accessibleRoomIds);
            buildRoomTree(accessibleRooms, allRooms, false);
            return null;
        });
    }

    private void buildRoomTree(List<WorkerRoomPair> accessibleRooms, List<Room> allRooms, boolean editMode) {
        // Фильтрация комнат
        List<Room> roomsToShow = allRooms.stream()
                .filter(room -> editMode || accessibleRoomIds.contains(room.getId()))
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

                if (editMode) {
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
            availableRoomsTree.setCellFactory(tv -> new TreeCell<String>() {
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
    private void editAccess() {
        allRoomsFuture.thenAccept(allRooms -> {
            accessibleFuture.thenAccept(accessibleRooms -> {
                Platform.runLater(() -> {
                    saveAccessBtn.setVisible(true);
                    buildRoomTree(accessibleRooms, allRooms, true);
                });
            });
        });
    }

    @FXML
    private void saveAccess() {
        // Только локальное обновление без сохранения на сервер
        accessibleRoomIds = new HashSet<>(currentlySelectedRoomIds);

        allRoomsFuture.thenAccept(allRooms -> {
            accessibleFuture.thenAccept(accessibleRooms -> {
                Platform.runLater(() -> {
                    saveAccessBtn.setVisible(false);
                    buildRoomTree(accessibleRooms, allRooms, false);
                });
            });
        });
    }

    private void fillForm() {
        if (worker != null) {
            nameField.setText(worker.getName());
            surnameField.setText(worker.getSurname());
            patronymicField.setText(worker.getPatronymic());
            sexComboBox.setValue(worker.getSex());
            birthdayPicker.setValue(worker.getBirthday());
            phoneField.setText(worker.getPhone());
            positionField.setText(worker.getPosition());
            otdelField.setText(worker.getOtdel());
            departmentField.setText(worker.getDepartment());
            HttpService.getWorkerPhoto(worker.getPhoto_file())
                    .thenAccept(loadedImage -> {
                        Platform.runLater(() -> {
                            if (loadedImage != null) {
                                photoView.setImage(loadedImage);
                            }
                        });
                    });
        }
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

        HttpService.setRoomsToWorker(accessibleRoomIds, worker.getId());
        HttpService.updateWorker(worker).thenAccept(success -> {
            if (success) {
                Platform.runLater(() -> {
                    if (refreshCallback != null) {
                        refreshCallback.run();
                    }
                    stage.close();
                });
            }
        });
        if (selectedFile != null) {
            HttpService.uploadWorkerPhoto(worker.getId(), selectedFile).thenAccept(uploadResult -> {
                if (uploadResult) {
                    System.out.println("загружено");
                }
            });
        }
    }

    @FXML
    private void deleteWorker() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Вы уверены, что хотите удалить сотрудника?");
        alert.setContentText(worker.getSurname() + " " + worker.getName() + " " + worker.getPatronymic());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            HttpService.deleteWorker(worker.getId()).thenAccept(success -> {
                if (success) {
                    Platform.runLater(() -> {
                        if (refreshCallback != null) {
                            refreshCallback.run();
                        }
                        stage.close();
                    });
                }
            });
        }
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