CREATE SEQUENCE booking_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE booking
(
    id        BIGINT DEFAULT nextval('booking_seq') PRIMARY KEY,
    room_id   BIGINT       NOT NULL REFERENCES room (id),
    booked_by VARCHAR(100) NOT NULL,
    starts_at TIMESTAMP    NOT NULL,
    ends_at   TIMESTAMP    NOT NULL,
    purpose   VARCHAR(200),
    -- UC-002 BR-001: A booking's end time is after its start time.
    CONSTRAINT booking_duration_ck CHECK (ends_at > starts_at),
    -- UC-002 BR-002: Bookings of the same room never overlap; [start, end) touching is allowed.
    CONSTRAINT booking_no_overlap_ex EXCLUDE USING gist (
        int8range(room_id, room_id, '[]') WITH =,
        tsrange(starts_at, ends_at, '[)') WITH &&
    )
);

CREATE INDEX booking_room_id_idx ON booking (room_id, starts_at);
