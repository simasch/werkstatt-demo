package ai.unifiedprocess.roombooking.room.service;

import java.util.List;

import jakarta.validation.Valid;

import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.domain.RoomRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Registration and overview of the meeting rooms. */
@Service
@Validated
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository repository;

    public RoomService(RoomRepository repository) {
        this.repository = repository;
    }

    /**
     * UC-001 step 2: lists every room with name, capacity, and equipment, sorted by name.
     */
    public List<Room> findAll() {
        return repository.findAll();
    }

    /**
     * UC-001 steps 5–7: registers a room; A1 when the name is already used, A2 when an entry is invalid.
     */
    @Transactional
    public Room register(@Valid RegisterRoom command) {
        // UC-001 BR-002: No two rooms have the same name, ignoring upper and lower case.
        if (repository.existsByName(command.name())) {
            throw new RoomNameAlreadyUsedException(command.name());
        }
        Room room = new Room(null, command.name(), command.capacity(), command.equipment());
        try {
            long id = repository.insert(room);
            return new Room(id, room.name(), room.capacity(), room.equipment());
        }
        catch (DuplicateKeyException ex) {
            // a concurrent registration of the same name passed the guard clause
            if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains("room_name_uk")) {
                throw new RoomNameAlreadyUsedException(command.name());
            }
            throw ex;
        }
    }
}
