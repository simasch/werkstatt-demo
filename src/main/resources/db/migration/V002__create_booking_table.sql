CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE SEQUENCE booking_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE booking
(
    id        BIGINT DEFAULT nextval('booking_seq') PRIMARY KEY,
    room_id   BIGINT       NOT NULL REFERENCES room (id),
    booked_by VARCHAR(100) NOT NULL,
    starts_at TIMESTAMP    NOT NULL,
    -- UC-002 BR-001: the end must be after the start.
    ends_at   TIMESTAMP    NOT NULL CHECK (ends_at > starts_at),
    purpose   VARCHAR(200),
    -- UC-002 BR-002: two bookings of the same room never overlap; one that ends when another starts does not
    -- overlap it.
    EXCLUDE USING gist (room_id WITH =, tsrange(starts_at, ends_at, '[)') WITH &&)
);
