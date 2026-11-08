-- UC-001 Register Room: test data. Fixed negative ids the room_seq sequence never hands out,
-- so this repeatable migration can run again on a database that already holds these rows.
INSERT INTO room (id, name, capacity, equipment)
VALUES (-101, 'Alpha Room', 4, 'Whiteboard'),
       (-102, 'Zulu Room', 12, NULL)
ON CONFLICT (id) DO NOTHING;
