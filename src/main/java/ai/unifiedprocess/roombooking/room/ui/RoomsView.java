package ai.unifiedprocess.roombooking.room.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.service.RegisterRoom;
import ai.unifiedprocess.roombooking.room.service.RoomNameAlreadyExistsException;
import ai.unifiedprocess.roombooking.room.service.RoomService;

/** UC-001 Register Room. */
@Route("rooms")
@PageTitle("Rooms")
@Menu(title = "Rooms")
public class RoomsView extends VerticalLayout {

    private final RoomService roomService;
    private final Grid<Room> grid = new Grid<>(Room.class, false);

    public RoomsView(RoomService roomService) {
        this.roomService = roomService;
        setSizeFull();

        Button newRoomButton = new Button("New Room", event -> openNewRoomDialog());
        newRoomButton.setId("new-room-button");

        grid.setId("room-grid");
        grid.addColumn(Room::name).setHeader("Name").setKey("name");
        grid.addColumn(Room::capacity).setHeader("Capacity").setKey("capacity");
        grid.addColumn(Room::equipment).setHeader("Equipment").setKey("equipment");
        grid.setSizeFull();

        add(newRoomButton, grid);
        refreshRooms();
    }

    private void refreshRooms() {
        grid.setItems(roomService.list());
    }

    /**
     * UC-001 steps 3–4: opens the empty room form.
     */
    private void openNewRoomDialog() {
        TextField nameField = new TextField("Name");
        nameField.setMaxLength(50);
        nameField.setId("room-name-field");

        IntegerField capacityField = new IntegerField("Capacity");
        capacityField.setMin(1);
        capacityField.setMax(50);
        capacityField.setId("room-capacity-field");

        TextField equipmentField = new TextField("Equipment");
        equipmentField.setMaxLength(200);
        equipmentField.setId("room-equipment-field");

        BeanValidationBinder<RegisterRoom> binder = new BeanValidationBinder<>(RegisterRoom.class);
        binder.forField(nameField).bind("name");
        binder.forField(capacityField).bind("capacity");
        binder.forField(equipmentField).bind("equipment");

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("New Room");
        dialog.add(new FormLayout(nameField, capacityField, equipmentField));

        Button saveButton = new Button("Save", event -> save(binder, nameField, dialog));
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);
        saveButton.setId("save-room-button");

        // UC-001 A3: closes the form without storing anything.
        Button cancelButton = new Button("Cancel", event -> dialog.close());
        cancelButton.setId("cancel-room-button");

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    /**
     * UC-001 steps 5–7: validates and stores the room; A1 when the name is already used.
     */
    private void save(BeanValidationBinder<RegisterRoom> binder, TextField nameField, Dialog dialog) {
        try {
            RegisterRoom command = binder.writeRecord();
            try {
                Room room = roomService.register(command);
                refreshRooms();
                dialog.close();
                Notification.show("Room " + room.name() + " registered.")
                        .addThemeVariants(NotificationVariant.SUCCESS);
            } catch (RoomNameAlreadyExistsException e) {
                // UC-001 A1: another room already has the name.
                nameField.setInvalid(true);
                nameField.setErrorMessage(e.getMessage());
            }
        } catch (ValidationException e) {
            // UC-001 A2: the Binder has already shown the message of each broken rule at its field.
        }
    }
}
