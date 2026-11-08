-- Test data of UC-001 Register Room: rooms registered before the office manager opens the overview.
-- Inserted out of name order so that the overview's order by name is observable.
INSERT INTO room (name, capacity, equipment) VALUES ('Lakeview', 8, 'projector, whiteboard') ON CONFLICT DO NOTHING;
INSERT INTO room (name, capacity, equipment) VALUES ('Atrium', 20, NULL) ON CONFLICT DO NOTHING;
INSERT INTO room (name, capacity, equipment) VALUES ('Harbour', 4, 'whiteboard') ON CONFLICT DO NOTHING;
