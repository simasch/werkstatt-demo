package ai.unifiedprocess.roombooking.room.ui;

import java.util.Map;

import ai.unifiedprocess.roombooking.room.domain.RoomListItem;
import ai.unifiedprocess.roombooking.room.service.RegisterRoom;
import ai.unifiedprocess.roombooking.room.service.RoomNameTakenException;
import ai.unifiedprocess.roombooking.room.service.RoomService;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

/** UC-001 Register Room: the room overview and the room form. */
@Route("rooms")
@RouteAlias("")
@PageTitle("Room Overview")
@Menu(title = "Rooms", order = 1)
public class RegisterRoomView extends VerticalLayout {

    public static final String NO_ROOM_YET = "No room is registered yet.";

    private final RoomService roomService;

    private final Grid<RoomListItem> grid = new Grid<>();

    public RegisterRoomView(RoomService roomService) {
        this.roomService = roomService;
        setSizeFull();

        Button register = new Button("Register Room", event -> new RoomDialog().open());
        register.addThemeVariants(ButtonVariant.PRIMARY);

        grid.setId("room-grid");
        grid.addColumn(RoomListItem::name).setHeader("Name").setKey("name");
        grid.addColumn(RoomListItem::capacity).setHeader("Capacity").setKey("capacity");
        grid.addColumn(RoomListItem::equipment).setHeader("Equipment").setKey("equipment");
        // UC-001 A1: an empty overview notes that no room is registered yet.
        grid.setEmptyStateText(NO_ROOM_YET);
        grid.setSizeFull();

        add(new H2("Room Overview"), register, grid);
        refresh();
    }

    private void refresh() {
        // UC-001 step 2: every registered room, ordered by name; a short, bounded list.
        grid.setItems(roomService.findAll());
    }

    /** UC-001 steps 4–7: the room form with Save and Cancel (A4). */
    private final class RoomDialog extends Dialog {

        private final TextField name = new TextField("Name");

        private final IntegerField capacity = new IntegerField("Capacity");

        private final TextArea equipment = new TextArea("Equipment");

        private final Binder<RegisterRoom> binder = new Binder<>(RegisterRoom.class);

        RoomDialog() {
            setHeaderTitle("Register Room");
            setId("room-dialog");

            name.setRequiredIndicatorVisible(true);
            capacity.setRequiredIndicatorVisible(true);
            capacity.setMin(1);
            capacity.setMax(50);
            capacity.setI18n(new IntegerField.IntegerFieldI18n()
                .setBadInputErrorMessage(RegisterRoom.CAPACITY_OUT_OF_RANGE)
                .setMinErrorMessage(RegisterRoom.CAPACITY_OUT_OF_RANGE)
                .setMaxErrorMessage(RegisterRoom.CAPACITY_OUT_OF_RANGE));
            equipment.setHelperText("For example: projector, whiteboard");

            binder.forField(name)
                // UC-001 BR-003: The name is required; spaces only count as missing (BR-002).
                .withValidator(value -> value != null && !value.isBlank(), RegisterRoom.NAME_REQUIRED)
                // UC-001 BR-005: The name has at most 50 characters, without outer spaces.
                .withValidator(value -> value.strip().length() <= 50, RegisterRoom.NAME_TOO_LONG)
                .bind("name");
            binder.forField(capacity)
                // UC-001 BR-003: The capacity is required.
                .asRequired(RegisterRoom.CAPACITY_REQUIRED)
                // UC-001 BR-004: The capacity is a whole number of people from 1 to 50.
                .withValidator(value -> value >= 1 && value <= 50, RegisterRoom.CAPACITY_OUT_OF_RANGE)
                .bind("capacity");
            binder.forField(equipment)
                // UC-001 BR-005: The equipment has at most 200 characters.
                .withValidator(value -> value == null || value.length() <= 200,
                        RegisterRoom.EQUIPMENT_TOO_LONG)
                .bind("equipment");

            FormLayout form = new FormLayout(name, capacity, equipment);
            form.setColspan(equipment, 2);
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
                // UC-001 A2: the binder marks each offending field; every entry stays in the form.
                return;
            }
            try {
                roomService.register(command);
            }
            catch (RoomNameTakenException ex) {
                // UC-001 A3: reported at the name field; every entry stays in the form.
                showError(name, ex.getMessage());
                return;
            }
            catch (ConstraintViolationException ex) {
                // UC-001 A2: the service rejected an entry the form let through.
                Map<String, HasValidation> fields = Map.of("name", name, "capacity", capacity,
                        "equipment", equipment);
                for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
                    HasValidation field = fields.get(lastNode(violation.getPropertyPath()));
                    if (field == null) {
                        throw ex;
                    }
                    showError(field, violation.getMessage());
                }
                return;
            }
            // UC-001 step 8: the new room appears in the room overview.
            close();
            refresh();
        }

        private static String lastNode(Path path) {
            String last = null;
            for (Path.Node node : path) {
                last = node.getName();
            }
            return last;
        }

        private static void showError(HasValidation field, String message) {
            field.setErrorMessage(message);
            field.setInvalid(true);
        }
    }
}
