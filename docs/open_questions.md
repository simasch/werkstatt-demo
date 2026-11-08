# Open Questions

Questions about the specification that the specification does not answer yet. A question is `Resolved` only when
the answer is in the specification itself; **Resolved in** names where. Resolved and withdrawn questions stay as
history.

## OQ-001: Where does the system show the double-booking message of A1?

**Element:** UC-002 A1
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** Yes
**Status:** Open

A1 says the system shows "The room is already booked from <start> to <end>." and keeps every entry of the form,
but it does not say where the message appears. UC-002 links NFR-001, which says rejected input "must be reported
at the offending field". Elsewhere the specification names the field when it means one: UC-001 A1 shows its
message "at the field "Name"", and UC-002 BR-001 shows "End must be after start." "at the field "End"". An overlap
involves the room and the whole time slot, so no single field is clearly the offending one.
Readings: (a) at the field "Start"; (b) at the field "End"; (c) at both "Start" and "End"; (d) at the field
"Room"; (e) as a message for the whole form (a notification, like the success message of step 5), which would
need NFR-001 to allow an exception for a rule that spans several fields. The reading decides what the UI shows
and what the tests of A1 assert.

## OQ-002: Which booking does the A1 message name when the time slot overlaps several bookings?

**Element:** UC-002 A1
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** Yes
**Status:** Open

A1 shows "The room is already booked from <start> to <end>." "with the start and end of the overlapping booking",
which assumes exactly one. A long time slot can overlap two or more bookings of the same room.
Readings: (a) the overlapping booking that starts first; (b) one message for each overlapping booking; (c) one
message that covers the earliest start to the latest end of all overlapping bookings. The reading decides the text
of the message and what the tests of A1 assert.
