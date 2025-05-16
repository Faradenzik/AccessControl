package by.farad.accesscontrol.controllers;

import by.farad.accesscontrol.models.MainJournal;
import by.farad.accesscontrol.models.Room;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class MainJournalController implements Initializable {
    @FXML private TableView<MainJournal> journalTable;
    @FXML private TableColumn<MainJournal, String> time;
    @FXML private TableColumn<MainJournal, String> fio;
    @FXML private TableColumn<MainJournal, String> otdel;
    @FXML private TableColumn<MainJournal, String> position;
    @FXML private TableColumn<MainJournal, String> room;
    @FXML private TableColumn<MainJournal, String> direction;
    @FXML private TableColumn<MainJournal, String> status;
    @FXML private TableColumn<MainJournal, String> description;
    @FXML private DatePicker startDate;
    @FXML private DatePicker endDate;
    @FXML private TextField sh;
    @FXML private TextField sm;
    @FXML private TextField eh;
    @FXML private TextField em;
    @FXML private ChoiceBox selectStatus;
    @FXML private ChoiceBox selectDirection;
    @FXML private ComboBox<String> selectRoom;
    @FXML private ComboBox<String> selectWorker;
    @FXML private ComboBox<String> selectOtdel;
    @FXML private ComboBox<String> selectPosition;

    private final ObservableList<MainJournal> journalData = FXCollections.observableArrayList();
    private final ObservableList<MainJournal> allJournalData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        time.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getTime().toLocalDate();
            LocalTime time = cellData.getValue().getTime().toLocalTime();
            String dateTime = String.format("%s %s", date,time);
            return new ReadOnlyStringWrapper(dateTime);
        });
        fio.setCellValueFactory(cellData -> {
            String fullName;
            Worker worker = cellData.getValue().getWorker();
            if (worker == null) {
                fullName = "Неизвестный";
            } else {
                fullName = String.format("%s %s %s", worker.getSurname(), worker.getName(), worker.getPatronymic());
            }
            return new ReadOnlyStringWrapper(fullName);
        });
        otdel.setCellValueFactory(cellData -> {
            String otdel = "";
            Worker worker = cellData.getValue().getWorker();
            if (worker != null) {
                otdel = worker.getOtdel();
            }
            return new ReadOnlyStringWrapper(otdel);
        });
        position.setCellValueFactory(cellData -> {
            String position = "";
            Worker worker = cellData.getValue().getWorker();
            if (worker != null) {
                position = worker.getPosition();
            }
            return new ReadOnlyStringWrapper(position);
        });
        room.setCellValueFactory(cellData -> {
            Room room = cellData.getValue().getRoom();
            String name = String.format("%s | %s этаж", room.getName(), room.getFloor());
            return new ReadOnlyStringWrapper(name);
        });
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        direction.setCellValueFactory(new PropertyValueFactory<>("direction"));
        description.setCellValueFactory(new PropertyValueFactory<>("description"));

        loadJournalData();
        journalTable.setItems(journalData);
        setupFiltering();
    }

    private void initializeFilters() {
        Set<String> workers = journalData.stream()
                .map(entry -> {
                    Worker w = entry.getWorker();
                    return w != null ? w.getSurname() + " " + w.getName() + " " + w.getPatronymic() : "Неизвестный";
                })
                .collect(Collectors.toCollection(TreeSet::new));
        selectWorker.setItems(FXCollections.observableArrayList(workers));

        // Отделы
        Set<String> otdels = journalData.stream()
                .map(entry -> entry.getWorker() != null ? entry.getWorker().getOtdel() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        selectOtdel.setItems(FXCollections.observableArrayList(otdels));

        // Должности
        Set<String> positions = journalData.stream()
                .map(entry -> entry.getWorker() != null ? entry.getWorker().getPosition() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        selectPosition.setItems(FXCollections.observableArrayList(positions));

        // Комнаты (в формате “Название (этаж N)”)
        Set<String> rooms = journalData.stream()
                .map(entry -> {
                    Room r = entry.getRoom();
                    return r.getName() + " | " + r.getFloor() + " этаж";
                })
                .collect(Collectors.toCollection(TreeSet::new));
        selectRoom.setItems(FXCollections.observableArrayList(rooms));
    }

    private void setupFiltering() {
        startDate.valueProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        endDate.valueProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        sh.textProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        sm.textProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        eh.textProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        em.textProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        selectStatus.setItems(FXCollections.observableArrayList("Не выбрано", "Успешно", "Заблокировано"));
        selectStatus.setValue("Не выбрано");
        selectStatus.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        selectDirection.setItems(FXCollections.observableArrayList("Не выбрано", "Вход", "Выход"));
        selectDirection.setValue("Не выбрано");
        selectDirection.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        selectRoom.getEditor().textProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        selectRoom.valueProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        selectWorker.getEditor().textProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        selectOtdel.getEditor().textProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        selectPosition.getEditor().textProperty().addListener((obs, oldVal, newVal) -> filterJournal());

        selectWorker.valueProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        selectOtdel.valueProperty().addListener((obs, oldVal, newVal) -> filterJournal());
        selectPosition.valueProperty().addListener((obs, oldVal, newVal) -> filterJournal());
    }

    private void filterJournal() {
        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();

        LocalTime startTime = parseTime(sh.getText(), sm.getText());
        LocalTime endTime = parseTime(eh.getText(), em.getText());

        String selectedStatus = (String) selectStatus.getValue();

        String selectedDirection = (String) selectDirection.getValue();

        String roomFilter = selectRoom.getEditor().getText().toLowerCase();

        String workerFilter = selectWorker.getEditor().getText().toLowerCase();
        String otdelFilter = selectOtdel.getEditor().getText().toLowerCase();
        String positionFilter = selectPosition.getEditor().getText().toLowerCase();

        journalData.setAll(allJournalData.filtered(entry -> {
            Worker worker = entry.getWorker();
            String fio;
            String otdel;
            String position;
            if (worker != null) {
                fio = String.format("%s %s %s",
                        worker.getSurname(),
                        worker.getName(),
                        worker.getPatronymic()).toLowerCase();

                otdel = worker.getOtdel() != null ? worker.getOtdel().toLowerCase() : "";
                position = worker.getPosition() != null ? worker.getPosition().toLowerCase() : "";
            } else {
                fio = "Неизвестный".toLowerCase();
                otdel = "";
                position = "";
            }
            Room room = entry.getRoom();
            String roomName = room.getName() != null ? room.getName().toLowerCase() : "";
            String roomFloor = String.valueOf(room.getFloor()).toLowerCase();

            boolean matchesRoom = roomFilter.isEmpty() ||
                    roomName.contains(roomFilter) ||
                    roomFloor.contains(roomFilter) ||
                    (roomName + " | " + roomFloor + " этаж").contains(roomFilter);

            boolean matchesFio = fio.contains(workerFilter);
            boolean matchesOtdel = otdel.contains(otdelFilter);
            boolean matchesPosition = position.contains(positionFilter);

            LocalDateTime dateTime = entry.getTime();
            LocalDate entryDate = dateTime.toLocalDate();
            LocalTime entryTime = dateTime.toLocalTime();

            boolean matchesStartDate = start == null || !entryDate.isBefore(start);
            boolean matchesEndDate = end == null || !entryDate.isAfter(end);
            boolean matchesStartTime = startTime == null || !entryTime.isBefore(startTime);
            boolean matchesEndTime = endTime == null || !entryTime.isAfter(endTime);

            String status = entry.getStatus() != null ? entry.getStatus() : "";
            boolean matchesStatus = selectedStatus.equals("Не выбрано") || status.equalsIgnoreCase(selectedStatus);

            String direction = entry.getDirection() != null ? entry.getDirection() : "";
            boolean matchesDirection = selectedDirection.equals("Не выбрано") || direction.equalsIgnoreCase(selectedDirection);

            return matchesFio && matchesOtdel && matchesPosition &&
                    matchesStartDate && matchesEndDate &&
                    matchesStartTime && matchesEndTime &&
                    matchesStatus && matchesDirection && matchesRoom;
        }));
    }

    public void loadJournalData() {
        HttpService.getMainJournal().thenAccept(journal -> {
            if (journal != null) {
                Platform.runLater(() -> {
                    journalData.clear();
                    journalData.addAll(journal);
                    allJournalData.setAll(journal);
                    journalTable.setItems(journalData);
                    initializeFilters(); // обновить списки работников, отделов, должностей и комнат
                    filterJournal();
                });
            }
        });

    }

    @FXML
    private void refreshData() {
        loadJournalData();
    }

    private LocalTime parseTime(String hourText, String minuteText) {
        try {
            int hour = Integer.parseInt(hourText);
            int minute = Integer.parseInt(minuteText);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
            return LocalTime.of(hour, minute);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
