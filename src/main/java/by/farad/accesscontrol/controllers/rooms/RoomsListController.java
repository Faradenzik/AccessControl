package by.farad.accesscontrol.controllers.rooms;

import by.farad.accesscontrol.models.Room;
import by.farad.accesscontrol.services.HttpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomsListController {
    @FXML
    private TreeView<String> roomsTree;

    private List<Room> allRooms;

    @FXML
    private void initialize() {
        loadRoomsData();

        roomsTree.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TreeItem<String> selectedItem = roomsTree.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.getParent() != null) { // не корень (этаж)
                    // Найдём комнату по тексту узла
                    String text = selectedItem.getValue(); // пример: "Комната 101 | ID123"
                    Room room = findRoomByDisplayText(text);
                    if (room != null) {
                        openEditWindow(room);
                    }
                }
            }
        });
    }

    private Room findRoomByDisplayText(String text) {
        if (allRooms == null) return null;
        for (Room room : allRooms) {
            String display = room.getName() + " | " + room.getDeviceId();
            if (display.equals(text)) return room;
        }
        return null;
    }

    private void loadRoomsData() {
        HttpService.getAllRooms().thenAccept(rooms -> {
            if (rooms != null) {
                Platform.runLater(() -> {
                    allRooms = rooms;
                    buildTreeView(rooms);
                });
            }
        });
    }

    private void buildTreeView(List<Room> rooms) {
        // Группируем по этажам
        Map<Integer, List<Room>> groupedByFloor = rooms.stream()
                .collect(Collectors.groupingBy(Room::getFloor));

        TreeItem<String> root = new TreeItem<>("Помещения");
        root.setExpanded(true);

        for (Map.Entry<Integer, List<Room>> entry : groupedByFloor.entrySet()) {
            TreeItem<String> floorItem = new TreeItem<>("Этаж " + entry.getKey());
            floorItem.setExpanded(true);

            for (Room room : entry.getValue()) {
                String roomDisplay = room.getName() + " | " + room.getDeviceId();
                TreeItem<String> roomItem = new TreeItem<>(roomDisplay);
                floorItem.getChildren().add(roomItem);
            }

            root.getChildren().add(floorItem);
        }

        roomsTree.setRoot(root);
        roomsTree.setShowRoot(false); // скрыть корень "Помещения" (по желанию)
    }

    private void openEditWindow(Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/rooms/room_edit.fxml"));
            Parent root = loader.load();

            RoomEditController controller = loader.getController();
            controller.setRoom(room);

            Stage stage = new Stage();
            stage.setTitle("Редактирование помещения");
            stage.setScene(new Scene(root));
            controller.setStage(stage);

            stage.setOnHidden(e -> loadRoomsData());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void addRoom() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/by/farad/accesscontrol/rooms/room_add.fxml"));
            Parent root = loader.load();

            RoomAddController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Создание помещения");
            stage.setScene(new Scene(root));
            controller.setStage(stage);

            stage.setOnHidden(e -> loadRoomsData());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
