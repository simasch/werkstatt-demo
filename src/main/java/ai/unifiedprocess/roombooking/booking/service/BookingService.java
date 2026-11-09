package ai.unifiedprocess.roombooking.booking.service;

import java.util.List;

import ai.unifiedprocess.roombooking.booking.domain.BookedTimeSlot;
import ai.unifiedprocess.roombooking.booking.domain.BookingRepository;
import ai.unifiedprocess.roombooking.booking.domain.NewBooking;
import ai.unifiedprocess.roombooking.room.domain.RoomListItem;
import ai.unifiedprocess.roombooking.room.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Booking of the meeting rooms. */
@Service
@Validated
@Transactional(readOnly = true)
public class BookingService {

    private static final String NO_OVERLAP_CONSTRAINT = "booking_no_overlap_ex";

    private final BookingRepository repository;

    private final RoomService roomService;

    public BookingService(BookingRepository repository, RoomService roomService) {
        this.repository = repository;
        this.roomService = roomService;
    }

    /** UC-002 step 2: every registered room, ordered by name; empty for A1. */
    public List<RoomListItem> findRooms() {
        return roomService.findAll();
    }

    /** UC-002 steps 4 and 8: the booked time slots of a room, ordered by start time. */
    public List<BookedTimeSlot> findBookedTimeSlots(long roomId) {
        return repository.findByRoom(roomId);
    }

    /**
     * UC-002 steps 6–7: checks the entries and stores the booking in one step; returns the new
     * id. Throws {@code ConstraintViolationException} and
     * {@link BookingEndNotAfterStartException} for A2, {@link RoomAlreadyBookedException} for A3.
     */
    @Transactional
    public long book(@Valid BookRoom command) {
        // UC-002 BR-001: A booking's end time must be after its start time.
        if (!command.endsAt().isAfter(command.startsAt())) {
            throw new BookingEndNotAfterStartException();
        }
        // UC-002 BR-002: No other booking of the same room overlaps the time slot.
        if (repository.existsOverlapping(command.roomId(), command.startsAt(), command.endsAt())) {
            throw new RoomAlreadyBookedException();
        }
        try {
            return repository.insert(new NewBooking(command.roomId(), command.bookedBy(),
                    command.startsAt(), command.endsAt(), command.purpose()));
        }
        catch (DataIntegrityViolationException ex) {
            // UC-002 BR-002: a booking saved meanwhile overlaps; the exclusion constraint rejects it.
            if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains(NO_OVERLAP_CONSTRAINT)) {
                throw new RoomAlreadyBookedException();
            }
            throw ex;
        }
    }
}
