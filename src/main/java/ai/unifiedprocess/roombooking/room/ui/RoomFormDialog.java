package ai.unifiedprocess.roombooking.room.ui;

import java.util.function.Consumer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.service.RegisterRoom;
import ai.unifiedprocess.roombooking.room.service.RoomNameAlreadyUsedException;
import ai.unifiedprocess.roombooking.room.service.RoomService;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

/** UC-001 steps 4–7: the room form for a new room, with the alternative flows A1 to A3. */
class RoomFormDialog extends Dialog {

    private final transient RoomService roomService;

    private final transient Consumer<Room> onRegistered;

    private final TextField name = new TextField("Name");

    private final IntegerField capacity = new IntegerField("Capacity");

    private final TextField equipment = new TextField("Equipment");

    private final Binder<RegisterRoom> binder = new Binder<>(RegisterRoom.class);

    RoomFormDialog(RoomService roomService, Consumer<Room> onRegistered) {
        this.roomService = roomService;
        this.onRegistered = onRegistered;
        setHeaderTitle("New Room");

        name.setRequiredIndicatorVisible(true);
        capacity.setRequiredIndicatorVisible(true);
        capacity.setMin(1);
        capacity.setMax(50);
        capacity.setI18n(new IntegerField.IntegerFieldI18n()
                .setBadInputErrorMessage(RegisterRoom.CAPACITY_RANGE)
                .setRequiredErrorMessage(RegisterRoom.CAPACITY_RANGE)
                .setMinErrorMessage(RegisterRoom.CAPACITY_RANGE)
                .setMaxErrorMessage(RegisterRoom.CAPACITY_RANGE));

        // UC-001 BR-001: Name required, at most 50 characters, spaces at both ends removed.
        binder.forField(name)
                .withValidator(value -> value != null && !value.strip().isEmpty(), RegisterRoom.NAME_REQUIRED)
                .withValidator(value -> value.strip().length() <= 50, RegisterRoom.NAME_TOO_LONG)
                .bind("name");
        // UC-001 BR-003: The capacity is required and a whole number from 1 to 50.
        binder.forField(capacity)
                .asRequired(RegisterRoom.CAPACITY_RANGE)
                .withValidator(value -> value >= 1 && value <= 50, RegisterRoom.CAPACITY_RANGE)
                .bind("capacity");
        // UC-001 BR-004: The equipment is optional and at most 200 characters long.
        binder.forField(equipment)
                .withValidator(value -> value == null || value.length() <= 200, RegisterRoom.EQUIPMENT_TOO_LONG)
                .bind("equipment");

        FormLayout form = new FormLayout(name, capacity, equipment);
        add(form);

        Button save = new Button("Save", event -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancel = new Button("Cancel", event -> close());
        getFooter().add(cancel, save);
    }

    private void save() {
        RegisterRoom command;
        try {
            command = binder.writeRecord();
        }
        catch (ValidationException ex) {
            // A2: the binder shows the message of each broken rule at its field
            return;
        }
        try {
            Room room = roomService.register(command);
            close();
            onRegistered.accept(room);
        }
        catch (RoomNameAlreadyUsedException ex) {
            showError(name, ex.getMessage());
        }
        catch (ConstraintViolationException ex) {
            for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
                HasValidation field = fieldOf(violation.getPropertyPath());
                if (field != null) {
                    showError(field, violation.getMessage());
                }
            }
        }
    }

    private HasValidation fieldOf(Path path) {
        String property = null;
        for (Path.Node node : path) {
            property = node.getName();
        }
        return switch (property == null ? "" : property) {
            case "name" -> name;
            case "capacity" -> capacity;
            case "equipment" -> equipment;
            default -> null;
        };
    }

    private static void showError(HasValidation field, String message) {
        field.setErrorMessage(message);
        field.setInvalid(true);
    }
}
