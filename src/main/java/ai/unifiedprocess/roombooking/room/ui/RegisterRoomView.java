package ai.unifiedprocess.roombooking.room.ui;

import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.service.RoomService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC-001 Register Room. Lists the rooms with name, capacity, and equipment, sorted by name, and opens the room form
 * for a new room.
 */
@Route("rooms")
@PageTitle("Rooms")
@Menu(title = "Rooms", order = 1)
public class RegisterRoomView extends VerticalLayout {

    private final transient RoomService roomService;

    private final Grid<Room> grid = new Grid<>(Room.class, false);

    public RegisterRoomView(RoomService roomService) {
        this.roomService = roomService;
        setSizeFull();

        Button newRoom = new Button("New Room", event -> openRoomForm());
        newRoom.addThemeVariants(ButtonVariant.PRIMARY);

        grid.setId("room-grid");
        grid.addColumn(Room::name).setHeader("Name").setKey("name");
        grid.addColumn(Room::capacity).setHeader("Capacity").setKey("capacity");
        grid.addColumn(Room::equipment).setHeader("Equipment").setKey("equipment");
        grid.setSizeFull();

        add(newRoom, grid);
        refreshGrid();
    }

    private void openRoomForm() {
        new RoomFormDialog(roomService, this::roomRegistered).open();
    }

    private void roomRegistered(Room room) {
        refreshGrid();
        Notification notification = Notification.show("Room " + room.name() + " registered.");
        notification.addThemeVariants(NotificationVariant.SUCCESS);
    }

    private void refreshGrid() {
        grid.setItems(roomService.findAll());
    }
}
