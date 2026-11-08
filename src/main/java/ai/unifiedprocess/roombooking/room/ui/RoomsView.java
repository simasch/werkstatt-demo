package ai.unifiedprocess.roombooking.room.ui;

import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.service.RegisterRoom;
import ai.unifiedprocess.roombooking.room.service.RoomNameAlreadyExistsException;
import ai.unifiedprocess.roombooking.room.service.RoomService;
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
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC-001 Register Room.
 */
@Route("rooms")
@PageTitle("Rooms")
@Menu(title = "Rooms")
public class RoomsView extends VerticalLayout {

    private final RoomService service;
    private final Grid<Room> grid = new Grid<>();

    public RoomsView(RoomService service) {
        this.service = service;

        grid.addColumn(Room::name).setHeader("Name");
        grid.addColumn(Room::capacity).setHeader("Capacity");
        grid.addColumn(Room::equipment).setHeader("Equipment");
        refreshGrid();

        Button newRoomButton = new Button("New Room", event -> openRegisterRoomDialog());

        setSizeFull();
        add(newRoomButton, grid);
        setFlexGrow(1, grid);
    }

    private void refreshGrid() {
        grid.setItems(service.list());
    }

    private void openRegisterRoomDialog() {
        TextField nameField = new TextField("Name");
        nameField.setMaxLength(50);
        IntegerField capacityField = new IntegerField("Capacity");
        TextField equipmentField = new TextField("Equipment");
        equipmentField.setMaxLength(200);

        Binder<RegisterRoom> binder = new Binder<>(RegisterRoom.class);
        // UC-001 BR-001: Name is required and at most 50 characters, trimmed before checking.
        binder.forField(nameField)
                .withValidator(name -> !name.strip().isEmpty(), "Name is required.")
                .withValidator(name -> name.strip().length() <= 50, "Name must be at most 50 characters.")
                .bind("name");
        // UC-001 BR-003: Capacity is required and a whole number from 1 to 50.
        binder.forField(capacityField)
                .withValidator(capacity -> capacity != null && capacity >= 1 && capacity <= 50,
                        "Capacity must be between 1 and 50.")
                .bind("capacity");
        // UC-001 BR-004: Equipment is optional and at most 200 characters.
        binder.forField(equipmentField)
                .withValidator(equipment -> equipment == null || equipment.length() <= 200,
                        "Equipment must be at most 200 characters.")
                .bind("equipment");

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("New Room");
        dialog.add(new FormLayout(nameField, capacityField, equipmentField));

        Button saveButton = new Button("Save", event -> save(binder, nameField, dialog));
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancelButton = new Button("Cancel", event -> dialog.close());
        dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }

    private void save(Binder<RegisterRoom> binder, TextField nameField, Dialog dialog) {
        RegisterRoom command;
        try {
            command = binder.writeRecord();
        } catch (ValidationException e) {
            return;
        }
        try {
            Room room = service.register(command);
            dialog.close();
            refreshGrid();
            Notification notification = Notification.show("Room " + room.name() + " registered.");
            notification.addThemeVariants(NotificationVariant.SUCCESS);
        } catch (RoomNameAlreadyExistsException e) {
            nameField.setErrorMessage(e.getMessage());
            nameField.setInvalid(true);
        }
    }
}
