CREATE SEQUENCE booking_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE booking
(
    id        BIGINT DEFAULT nextval('booking_seq') PRIMARY KEY,
    room_id   BIGINT       NOT NULL REFERENCES room (id),
    booked_by VARCHAR(100) NOT NULL,
    starts_at TIMESTAMP    NOT NULL,
    ends_at   TIMESTAMP    NOT NULL,
    purpose   VARCHAR(200),
    -- UC-002 BR-001: The end of a booking is after its start.
    CHECK (ends_at > starts_at)
);

CREATE FUNCTION check_booking_no_overlap() RETURNS TRIGGER AS
$$
BEGIN
    -- serialize concurrent bookings of the same room so the check below cannot race
    PERFORM pg_advisory_xact_lock(NEW.room_id);
    IF EXISTS (SELECT 1
               FROM booking
               WHERE room_id = NEW.room_id
                 AND id <> COALESCE(NEW.id, -1)
                 AND NEW.starts_at < ends_at
                 AND NEW.ends_at > starts_at) THEN
        RAISE EXCEPTION 'booking for room % overlaps an existing booking', NEW.room_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- UC-002 BR-002: Two bookings of the same room never overlap; one that ends when another starts does not overlap it.
CREATE TRIGGER booking_no_overlap_trg
    BEFORE INSERT OR UPDATE ON booking
    FOR EACH ROW EXECUTE FUNCTION check_booking_no_overlap();
