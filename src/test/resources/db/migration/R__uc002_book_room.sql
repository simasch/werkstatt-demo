-- Test data of UC-002 Book Room: two rooms of their own, so that the bookings never touch the rooms of UC-001,
-- and two bookings of the room Weisshorn on 2026-11-09. The room Dom has no booking.
-- Fixed ids far above what room_seq and booking_seq hand out; ON CONFLICT keeps the repeatable migration re-runnable.
INSERT INTO room (id, name, capacity, equipment)
VALUES (900011, 'Weisshorn', 8, 'Whiteboard'),
       (900012, 'Dom', 4, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO booking (id, room_id, booked_by, starts_at, ends_at, purpose)
VALUES (900001, 900011, 'Anna Keller', '2026-11-09 10:00', '2026-11-09 11:00', 'Sprint review'),
       (900002, 900011, 'Ben Huber', '2026-11-09 13:00', '2026-11-09 14:00', NULL)
ON CONFLICT DO NOTHING;
