package ai.unifiedprocess.roombooking.room.domain;

import static ai.unifiedprocess.roombooking.database.Tables.ROOM;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Reads and writes the rooms. */
@Repository
public class RoomRepository {

    private final DSLContext ctx;

    public RoomRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /** Every registered room, ordered by name. */
    public List<RoomListItem> findAll() {
        return ctx.select(ROOM.ID, ROOM.NAME, ROOM.CAPACITY, ROOM.EQUIPMENT)
                .from(ROOM)
                .orderBy(ROOM.NAME.lower(), ROOM.NAME, ROOM.ID)
                .fetch(Records.mapping(RoomListItem::new));
    }

    /**
     * Whether a room with this name exists, compared without regard to upper and lower case.
     * The name must already be trimmed.
     */
    public boolean existsByName(String name) {
        // UC-001 BR-001: Room names are compared without regard to case.
        return ctx.fetchExists(ROOM, ROOM.NAME.lower().eq(DSL.lower(DSL.val(name))));
    }

    /**
     * Stores a new room and returns its id. Throws {@code DuplicateKeyException} when another
     * room already has the name (unique index {@code room_name_uk}).
     */
    public long insert(NewRoom room) {
        return ctx.insertInto(ROOM)
                .set(ROOM.NAME, room.name())
                .set(ROOM.CAPACITY, room.capacity())
                .set(ROOM.EQUIPMENT, room.equipment())
                .returning(ROOM.ID)
                .fetchSingle(ROOM.ID);
    }
}
