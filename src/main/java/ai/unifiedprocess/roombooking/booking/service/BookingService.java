package ai.unifiedprocess.roombooking.booking.service;

import jakarta.validation.Valid;

import ai.unifiedprocess.roombooking.booking.domain.Booking;
import ai.unifiedprocess.roombooking.booking.domain.BookingRepository;
import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.domain.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Booking of the meeting rooms. */
@Service
@Validated
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;

    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    /**
     * UC-002 steps 4–5: books a room for a time slot; A1 when the time slot overlaps another booking of the room,
     * A2 when an entry is invalid.
     * @return the stored booking
     */
    @Transactional
    public Booking book(@Valid BookRoom command) {
        // UC-002 BR-001: The end of the time slot is after its start.
        if (!command.endsAt().isAfter(command.startsAt())) {
            throw new EndNotAfterStartException();
        }
        // the lock on the room serializes concurrent bookings of the same room, so BR-002 cannot be raced
        Room room = roomRepository.findByIdForUpdate(command.roomId())
            .orElseThrow(() -> new IllegalArgumentException("No room with id " + command.roomId()));
        // UC-002 BR-002: Two bookings of the same room never overlap; touching ends do not.
        bookingRepository.findFirstOverlapping(room.id(), command.startsAt(), command.endsAt())
            .ifPresent(overlapping -> {
                throw new RoomAlreadyBookedException(overlapping);
            });
        Booking booking = new Booking(null, room.id(), command.bookedBy(), command.startsAt(), command.endsAt(),
                command.purpose());
        long id = bookingRepository.insert(booking);
        return new Booking(id, booking.roomId(), booking.bookedBy(), booking.startsAt(), booking.endsAt(),
                booking.purpose());
    }
}
