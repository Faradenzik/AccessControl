package by.farad.accesscontrol.models;

import javafx.scene.control.TreeItem;

public class RoomTreeItem extends TreeItem<String> {
    private Room room; // Может быть null для узлов-групп (этажей)

    public RoomTreeItem(Integer floor) {
        super(floor.toString()); // Для узлов-групп (этажей)
    }

    public RoomTreeItem(Room room) {
        super(room.getName()); // Для листьев (комнат)
        this.room = room;
    }

    public boolean isRoom() {
        return room != null;
    }
}