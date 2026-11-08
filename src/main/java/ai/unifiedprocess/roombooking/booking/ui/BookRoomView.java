package ai.unifiedprocess.roombooking.booking.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import ai.unifiedprocess.roombooking.booking.domain.Booking;
import ai.unifiedprocess.roombooking.booking.service.BookRoom;
import ai.unifiedprocess.roombooking.booking.service.BookingService;
import ai.unifiedprocess.roombooking.booking.service.RoomAlreadyBookedException;
import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.service.RoomService;

/** UC-002 Book Room. */
@Route("book-room")
@PageTitle("Book Room")
@Menu(title = "Book Room")
public class BookRoomView extends VerticalLayout {

    // UC-002 BR-004: dates in messages are shown as yyyy-MM-dd HH:mm.
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingService bookingService;

    private final ComboBox<Room> roomField = new ComboBox<>("Room");
    private final TextField bookedByField = new TextField("Booked by");
    private final DateTimePicker startField = new DateTimePicker("Start");
    private final DateTimePicker endField = new DateTimePicker("End");
    private final TextField purposeField = new TextField("Purpose");
    private final BeanValidationBinder<BookRoom> binder = new BeanValidationBinder<>(BookRoom.class);

    public BookRoomView(BookingService bookingService, RoomService roomService) {
        this.bookingService = bookingService;
        setSizeFull();

        List<Room> rooms = roomService.list();
        roomField.setId("booking-room-field");
        roomField.setItems(rooms);
        roomField.setItemLabelGenerator(Room::name);

        bookedByField.setId("booking-booked-by-field");
        bookedByField.setMaxLength(100);
        // UC-002 BR-003: the component's own constraint message replaces the Binder's Bean Validation
        // message when the value exceeds maxLength, so it must say the same thing.
        bookedByField.setI18n(new TextField.TextFieldI18n()
                .setMaxLengthErrorMessage("Booked by must be at most 100 characters."));

        startField.setId("booking-start-field");
        endField.setId("booking-end-field");

        purposeField.setId("booking-purpose-field");
        purposeField.setMaxLength(200);
        // UC-002 BR-003: see the comment on bookedByField's i18n above.
        purposeField.setI18n(new TextField.TextFieldI18n()
                .setMaxLengthErrorMessage("Purpose must be at most 200 characters."));

        binder.forField(roomField)
                .withConverter(room -> room == null ? null : room.id(),
                        id -> rooms.stream().filter(room -> room.id().equals(id)).findFirst().orElse(null))
                .bind("roomId");
        binder.forField(bookedByField).bind("bookedBy");
        Binder.Binding<BookRoom, LocalDateTime> startBinding = binder.forField(startField).bind("startsAt");
        binder.forField(endField)
                // UC-002 BR-001: the end must be after the start.
                .withValidator(end -> {
                    LocalDateTime start = startBinding.valueSignal().get();
                    return start == null || end == null || end.isAfter(start);
                }, "End must be after start.")
                .bind("endsAt");
        binder.forField(purposeField).bind("purpose");

        Button bookButton = new Button("Book", event -> book());
        bookButton.addThemeVariants(ButtonVariant.PRIMARY);
        bookButton.setId("book-room-button");

        add(new FormLayout(roomField, bookedByField, startField, endField, purposeField), bookButton);
    }

    /**
     * UC-002 steps 3–5: validates and stores the booking; A1 when the room is already booked, A2 when an entry is
     * invalid.
     */
    private void book() {
        try {
            BookRoom command = binder.writeRecord();
            try {
                Room room = roomField.getValue();
                Booking booking = bookingService.book(command);
                Notification.show("Room " + room.name() + " booked from " + booking.startsAt().format(DATE_TIME_FORMAT)
                        + " to " + booking.endsAt().format(DATE_TIME_FORMAT) + ".")
                        .addThemeVariants(NotificationVariant.SUCCESS);
                roomField.clear();
                bookedByField.clear();
                startField.clear();
                endField.clear();
                purposeField.clear();
            } catch (RoomAlreadyBookedException e) {
                // UC-002 A1: the time slot overlaps another booking of the same room; every entry is kept.
                Notification.show(e.getMessage()).addThemeVariants(NotificationVariant.ERROR);
            }
        } catch (ValidationException e) {
            // UC-002 A2: the Binder has already shown the message of each broken rule at its field.
        }
    }
}
