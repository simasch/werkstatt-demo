package ai.unifiedprocess.roombooking.booking.service;

import ai.unifiedprocess.roombooking.booking.domain.Booking;
import ai.unifiedprocess.roombooking.booking.domain.BookingRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Booking of the office's meeting rooms.
 */
@Service
@Validated
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    /**
     * UC-002 steps 3-5: books a room; A1 when the time slot overlaps another booking, A2 when an entry is invalid.
     */
    @Transactional
    public Booking book(@Valid BookRoom command) {
        // UC-002 BR-001: The end of a booking is after its start.
        if (!command.endsAt().isAfter(command.startsAt())) {
            throw new EndNotAfterStartException();
        }
        // UC-002 BR-002: Two bookings of the same room never overlap.
        repository.findOverlapping(command.roomId(), command.startsAt(), command.endsAt()).ifPresent(overlapping -> {
            throw new RoomAlreadyBookedException(overlapping.startsAt(), overlapping.endsAt());
        });
        Booking booking = new Booking(null, command.roomId(), command.bookedBy(), command.startsAt(),
                command.endsAt(), command.purpose());
        try {
            long id = repository.insert(booking);
            return new Booking(id, booking.roomId(), booking.bookedBy(), booking.startsAt(), booking.endsAt(),
                    booking.purpose());
        } catch (DataIntegrityViolationException e) {
            // a concurrent booking of the same room and time slot passed the guard clause above
            Booking overlapping = repository.findOverlapping(command.roomId(), command.startsAt(), command.endsAt())
                    .orElseThrow(() -> e);
            throw new RoomAlreadyBookedException(overlapping.startsAt(), overlapping.endsAt());
        }
    }
}
