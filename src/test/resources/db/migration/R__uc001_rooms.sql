-- UC-001 Register Room: shared test data, sorted by name (case-insensitive) as the room list shows it.
INSERT INTO room (name, capacity, equipment)
VALUES ('Atrium', 8, 'Projector, Whiteboard'),
       ('Boardroom', 4, NULL)
ON CONFLICT DO NOTHING;
