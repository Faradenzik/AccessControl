package by.farad.accesscontrol.controllers.access;

import by.farad.accesscontrol.models.AccessTimeRange;
import by.farad.accesscontrol.models.Room;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AccessRangesController {
    @FXML private Label label;
    @FXML private TableView<AccessTimeRange> rangesTable;
    @FXML private TableColumn<AccessTimeRange, String> type;
    @FXML private TableColumn<AccessTimeRange, String> grwr;
    @FXML private TableColumn<AccessTimeRange, String> days;
    @FXML private TableColumn<AccessTimeRange, String> timeStart;
    @FXML private TableColumn<AccessTimeRange, String> timeEnd;
    @FXML private TreeView<String> floorTree;

    private ObservableList<AccessTimeRange> allRanges = FXCollections.observableArrayList();
    private FilteredList<AccessTimeRange> filteredRanges = new FilteredList<>(allRanges, p -> true);

    private static final Map<String, String> dayNames = Map.of(
            "1", "Пн",
            "2", "Вт",
            "3", "Ср",
            "4", "Чт",
            "5", "Пт",
            "6", "Сб",
            "7", "Вс"
    );

    @FXML
    private void initialize() {
        rangesTable.setRowFactory(tv -> {
            TableRow<AccessTimeRange> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    AccessTimeRange range = row.getItem();
                    openEditWindow(range);
                }
            });
            return row;
        });

        setupFloorTreeListener();
        loadRoomTree();

        type.setCellValueFactory(cellData -> {
            AccessTimeRange range = cellData.getValue();

            if (range.getWorker() != null) {
                return new SimpleStringProperty("Сотрудник");
            } else {
                return new SimpleStringProperty("Группа");
            }
        });
        grwr.setCellValueFactory(cellData -> {
            AccessTimeRange range = cellData.getValue();
            if (range.getWorker() != null) {
                return new SimpleStringProperty(
                        range.getWorker().getSurname() + " " +
                                range.getWorker().getName() + " " +
                                range.getWorker().getPatronymic()
                );
            } else {
                return new SimpleStringProperty(range.getGroup().getName());
            }
        });
        days.setCellValueFactory(cellData -> {
            String raw = cellData.getValue().getDaysOfWeek();

            if (raw.equals("0")) {
                return new SimpleStringProperty("Блокировка доступа");
            }

            String[] dayNumbers = raw.split("");
            String formatted = Arrays.stream(dayNumbers)
                    .map(dayNames::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));

            return new SimpleStringProperty(formatted);
        });

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        timeStart.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTimeStart().format(timeFormatter))
        );
        timeEnd.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTimeEnd().format(timeFormatter))
        );

        SortedList<AccessTimeRange> sortedRanges = new SortedList<>(filteredRanges);
        sortedRanges.comparatorProperty().bind(rangesTable.comparatorProperty());
        rangesTable.setItems(sortedRanges);

        loadAccessTimeRanges();
    }

    private void openEditWindow(AccessTimeRange range) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/access/range_edit.fxml"));
            Parent root = loader.load();

            RangeEditController controller = loader.getController();
            controller.setRange(range);

            Stage stage = new Stage();
            stage.setTitle("Редактирование расписания");
            stage.setScene(new Scene(root));
            controller.setStage(stage);

            stage.setOnHidden(e -> loadAccessTimeRanges());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupFloorTreeListener() {
        floorTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String selected = newVal.getValue();
                label.setText("Расписание доступа в помещение: " + selected);
                filteredRanges.setPredicate(range -> {
                    if (selected.equals("Все помещения")) {
                        return true;
                    }
                    if (selected.startsWith("Этаж ")) {
                        return selected.equals("Этаж " + range.getRoom().getFloor());
                    }
                    return selected.equals(range.getRoom().getName());
                });
            }
        });
    }

    private void loadAccessTimeRanges() {
        HttpService.getAccessTimeRanges().thenAccept(ranges -> {
            if (ranges != null) {
                Platform.runLater(() -> {
                    allRanges.setAll(ranges);
                });
            }
        });
    }

    private void loadRoomTree() {
        HttpService.getAllRooms().thenAccept(rooms -> {
            if (rooms == null) return;

            // Группировка помещений по этажам
            Map<Integer, List<Room>> roomsByFloor = rooms.stream()
                    .collect(Collectors.groupingBy(Room::getFloor));

            Platform.runLater(() -> {
                TreeItem<String> root = new TreeItem<>("Все помещения");
                root.setExpanded(true);

                for (Map.Entry<Integer, List<Room>> entry : roomsByFloor.entrySet()) {
                    Integer floor = entry.getKey();
                    List<Room> roomsOnFloor = entry.getValue();

                    TreeItem<String> floorItem = new TreeItem<>("Этаж " + floor);

                    for (Room room : roomsOnFloor) {
                        TreeItem<String> roomItem = new TreeItem<>(room.getName());
                        floorItem.getChildren().add(roomItem);
                    }

                    root.getChildren().add(floorItem);
                }

                floorTree.setRoot(root);
            });
        });
    }

    public void addRule() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/access/range_add.fxml"));
            Parent root = loader.load();

            RangeAddController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Создание расписания");
            stage.setScene(new Scene(root));
            controller.setStage(stage);

            stage.setOnHidden(e -> loadAccessTimeRanges());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
