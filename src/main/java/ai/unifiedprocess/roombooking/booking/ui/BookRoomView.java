package ai.unifiedprocess.roombooking.booking.ui;

import ai.unifiedprocess.roombooking.booking.domain.Booking;
import ai.unifiedprocess.roombooking.booking.service.BookRoom;
import ai.unifiedprocess.roombooking.booking.service.BookingService;
import ai.unifiedprocess.roombooking.booking.service.EndNotAfterStartException;
import ai.unifiedprocess.roombooking.booking.service.RoomAlreadyBookedException;
import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.service.RoomService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * UC-002 Book Room.
 */
@Route("book-room")
@PageTitle("Book Room")
@Menu(title = "Book Room")
public class BookRoomView extends VerticalLayout {

    // UC-002 BR-004: Messages show a date and time as yyyy-MM-dd HH:mm.
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingService service;
    private final List<Room> rooms;

    private final ComboBox<Room> roomField = new ComboBox<>("Room");
    private final TextField bookedByField = new TextField("Booked by");
    private final DateTimePicker startField = new DateTimePicker("Start");
    private final DateTimePicker endField = new DateTimePicker("End");
    private final TextField purposeField = new TextField("Purpose");
    private final Binder<BookRoom> binder = new Binder<>(BookRoom.class);

    public BookRoomView(BookingService service, RoomService roomService) {
        this.service = service;
        this.rooms = roomService.list();

        roomField.setItems(rooms);
        roomField.setItemLabelGenerator(Room::name);
        bookedByField.setMaxLength(100);
        purposeField.setMaxLength(200);

        // UC-002 BR-003: Room is required.
        binder.forField(roomField).withConverter(new Converter<Room, Long>() {
            @Override
            public Result<Long> convertToModel(Room room, ValueContext context) {
                return room == null ? Result.error("Room is required.") : Result.ok(room.id());
            }

            @Override
            public Room convertToPresentation(Long id, ValueContext context) {
                return rooms.stream().filter(room -> room.id().equals(id)).findFirst().orElse(null);
            }
        }).bind("roomId");
        // UC-002 BR-003: Booked by is required and at most 100 characters, trimmed first.
        binder.forField(bookedByField)
                .withValidator(bookedBy -> !bookedBy.strip().isEmpty(), "Booked by is required.")
                .withValidator(bookedBy -> bookedBy.strip().length() <= 100,
                        "Booked by must be at most 100 characters.")
                .bind("bookedBy");
        // UC-002 BR-001: Start is required.
        binder.forField(startField).asRequired("Start is required.").bind("startsAt");
        // UC-002 BR-001: End is required.
        binder.forField(endField)
                .asRequired("End is required.")
                // UC-002 BR-001: The end of a booking is after its start.
                .withValidator(end -> startField.isEmpty() || end.isAfter(startField.getValue()),
                        "End must be after start.")
                .bind("endsAt");
        // UC-002 BR-003: Purpose is optional and at most 200 characters.
        binder.forField(purposeField)
                .withValidator(purpose -> purpose == null || purpose.length() <= 200,
                        "Purpose must be at most 200 characters.")
                .bind("purpose");

        Button bookButton = new Button("Book", event -> book());
        bookButton.addThemeVariants(ButtonVariant.PRIMARY);

        add(new FormLayout(roomField, bookedByField, startField, endField, purposeField), bookButton);
    }

    private void book() {
        BookRoom command;
        try {
            command = binder.writeRecord();
        } catch (ValidationException e) {
            return;
        }
        try {
            Booking booking = service.book(command);
            Room room = roomField.getValue();
            Notification notification = Notification.show("Room " + room.name() + " booked from "
                    + booking.startsAt().format(FORMAT) + " to " + booking.endsAt().format(FORMAT) + ".");
            notification.addThemeVariants(NotificationVariant.SUCCESS);
            clear();
        } catch (RoomAlreadyBookedException e) {
            Notification notification = Notification.show(e.getMessage());
            notification.addThemeVariants(NotificationVariant.ERROR);
        } catch (EndNotAfterStartException e) {
            endField.setErrorMessage(e.getMessage());
            endField.setInvalid(true);
        }
    }

    private void clear() {
        roomField.clear();
        bookedByField.clear();
        startField.clear();
        endField.clear();
        purposeField.clear();
    }
}
