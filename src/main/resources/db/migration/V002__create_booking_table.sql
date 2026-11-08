CREATE SEQUENCE booking_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE booking
(
    id        BIGINT DEFAULT nextval('booking_seq') PRIMARY KEY,
    room_id   BIGINT       NOT NULL REFERENCES room (id),
    -- UC-002 BR-003: Booked by is required and stored without leading or trailing spaces.
    booked_by VARCHAR(100) NOT NULL CHECK (booked_by = btrim(booked_by) AND booked_by <> ''),
    starts_at TIMESTAMP    NOT NULL,
    ends_at   TIMESTAMP    NOT NULL,
    purpose   VARCHAR(200),
    -- UC-002 BR-001: The end of the time slot is after its start.
    CONSTRAINT booking_time_slot_ck CHECK (ends_at > starts_at),
    -- UC-002 BR-002: Two bookings of the same room never overlap; touching ends do not.
    CONSTRAINT booking_no_overlap_ex EXCLUDE USING gist (
        int8range(room_id, room_id, '[]') WITH =,
        tsrange(starts_at, ends_at, '[)') WITH &&)
);

CREATE INDEX booking_room_id_ix ON booking (room_id);
