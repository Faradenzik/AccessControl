package by.farad.accesscontrol.controllers.access;

import by.farad.accesscontrol.models.AccessGroup;
import by.farad.accesscontrol.models.AccessTimeRange;
import by.farad.accesscontrol.models.Room;
import by.farad.accesscontrol.models.Worker;
import by.farad.accesscontrol.services.HttpService;
import com.gluonhq.charm.glisten.control.ToggleButtonGroup;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RangeEditController {
    @FXML
    private ToggleButton groupBtn;
    @FXML private ToggleButton workerBtn;
    @FXML private ToggleButtonGroup days;
    @FXML private ToggleButton monday;
    @FXML private ToggleButton tuesday;
    @FXML private ToggleButton wednesday;
    @FXML private ToggleButton thursday;
    @FXML private ToggleButton friday;
    @FXML private ToggleButton saturday;
    @FXML private ToggleButton sunday;
    @FXML private ChoiceBox<Room> roomList;
    @FXML private TextField sh;
    @FXML private TextField sm;
    @FXML private TextField eh;
    @FXML private TextField em;
    @FXML private ChoiceBox listObj;

    private final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private final ObservableList<Worker> workers = FXCollections.observableArrayList();
    private final ObservableList<AccessGroup> allGroups = FXCollections.observableArrayList();

    private AccessTimeRange group = new AccessTimeRange();

    @Setter
    private Stage stage;
    @Setter
    private Runnable refreshCallback;
    private AccessTimeRange accessRange;

    public void setRange(AccessTimeRange range) {
        this.accessRange = range;
        fillform();
    }

    private void fillform() {
        if (accessRange != null) {
            roomList.setValue(accessRange.getRoom());
            if (accessRange.getGroup() != null) {
                groupBtn.setSelected(true);
                listObj.setItems(allGroups);
                listObj.setValue(accessRange.getGroup());
            } else {
                workerBtn.setSelected(true);
                listObj.setItems(workers);
                listObj.setValue(accessRange.getWorker());
            }
            List<ToggleButton> groupBtns = List.of(monday, tuesday, wednesday, thursday, friday, saturday, sunday);
            List<Integer> daysWeek = accessRange.getDaysOfWeek().chars()
                    .mapToObj(c -> Character.getNumericValue((char) c))
                    .collect(Collectors.toList());
            for (int i = 0; i < groupBtns.size(); i++) {
                ToggleButton button = groupBtns.get(i);
                button.setSelected(daysWeek.contains(i+1));
            }
            sh.setText(String.valueOf(accessRange.getTimeStart().getHour()));
            sm.setText(String.valueOf(accessRange.getTimeStart().getMinute()));
            eh.setText(String.valueOf(accessRange.getTimeEnd().getHour()));
            em.setText(String.valueOf(accessRange.getTimeEnd().getMinute()));
        }
    }

    @FXML
    private void initialize() {
        loadRooms();
        loadWorkers();
        loadGroups();

        listObj.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Object obj) {
                try {
                    if (obj instanceof Worker worker) {
                        return worker.getSurname() + " " + worker.getName() + " " + worker.getPatronymic();
                    } else if (obj instanceof AccessGroup group) {
                        return group.getName();
                    }
                    return "";
                } catch (NullPointerException e) {
                    return "";
                }
            }

            @Override
            public Worker fromString(String s) {
                return null;
            }
        });

        roomList.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Room room) {
                try {
                    return room.getName() + ", " + room.getFloor() + " этаж";
                } catch (NullPointerException e) {
                    return "";
                }
            }

            @Override
            public Room fromString(String s) {
                return null;
            }
        });

        roomList.setItems(rooms);
    }

    private void loadGroups() {
        HttpService.getAllAccessGroups().thenAccept(groups -> {
            if (groups != null) {
                Platform.runLater(() -> {
                    allGroups.clear();
                    allGroups.addAll(groups);
                });
            }
        });
    }

    private void loadRooms() {
        HttpService.getAllRooms().thenAccept(rooms -> {
            if (rooms != null) {
                Platform.runLater(() -> {
                    this.rooms.clear();
                    this.rooms.addAll(rooms);
                });
            }
        });
    }

    public void loadWorkers() {
        HttpService.getAllWorkers().thenAccept(workers -> {
            if (workers != null) {
                Platform.runLater(() -> {
                    this.workers.clear();
                    this.workers.addAll(workers);
                });
            }
        });
    }

    @FXML
    private void saveRange () {
        group.setRoom(roomList.getValue());

        if (workerBtn.isSelected()) {
            group.setWorker((Worker) listObj.getValue());
        } else if (groupBtn.isSelected()) {
            group.setGroup((AccessGroup) listObj.getValue());
        } /*TODO проверка на отсутствие*/

        StringBuilder daysOfWeek = new StringBuilder();
        if (monday.isSelected())    daysOfWeek.append("1");
        if (tuesday.isSelected())   daysOfWeek.append("2");
        if (wednesday.isSelected()) daysOfWeek.append("3");
        if (thursday.isSelected())  daysOfWeek.append("4");
        if (friday.isSelected())    daysOfWeek.append("5");
        if (saturday.isSelected())  daysOfWeek.append("6");
        if (sunday.isSelected())    daysOfWeek.append("7");
        group.setDaysOfWeek(daysOfWeek.toString());

        int startH = Integer.parseInt(sh.getText());
        int startM = Integer.parseInt(sm.getText());
        int endH = Integer.parseInt(eh.getText());
        int endM = Integer.parseInt(em.getText());

        LocalTime startTime = LocalTime.of(startH, startM);
        LocalTime endTime = LocalTime.of(endH, endM);
        group.setTimeStart(startTime);
        group.setTimeEnd(endTime);

        HttpService.addAccessRange(group);
    }

    @FXML
    private void setWorkerBtn() {
        listObj.setItems(workers);
    }

    @FXML
    private void setGroupBtn() {
        listObj.setItems(allGroups);
    }

    @FXML
    public void deleteRange() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Вы уверены, что хотите удалить расписание?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            HttpService.deleteRange(accessRange.getId()).thenAccept(success -> {
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
}
