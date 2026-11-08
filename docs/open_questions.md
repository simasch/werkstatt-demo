# Open Questions

Questions about the specification that the specification does not answer yet. A question is `Resolved` only when
the answer is in the specification itself; **Resolved in** names where. Resolved and withdrawn questions stay as
history.

## OQ-001: What message and form behaviour does a BR-001 violation (end not after start) show?

**Element:** UC-002 BR-001
**Raised by:** /use-case-implement, 2026-11-08
**Blocking:** Yes
**Status:** Open

Step 4 validates the entries against BR-001 and BR-002, but only one alternative flow follows: A1, for BR-002
("Room Already Booked"), with its exact message text ("The room is already booked from <start> to <end>.") and
the instruction to keep every entry of the form. BR-001 ("The end of a booking is after its start.") has no
alternative flow and no message text at all — unlike UC-001, where every business rule (BR-001, BR-003, BR-004)
states its own rejection message in the rule itself (e.g. "An empty name is rejected with 'Name is required.'").

Readings seen: (a) a field-level message at "End", worded like UC-001's field messages, e.g. "End must be after
start."; (b) a form-level notification analogous to A1's wording, e.g. "The end must be after the start."; (c) the
field-level message also names the field in error ("Start" vs "End"); (d) some other wording. NFR-001 fixes the
mechanism (reported at the offending field, every entry kept) but not the wording, and this is a user-visible
message with business meaning the specification does not state — not a layout or label choice the agent may decide
alone. Without it, the validation for BR-001 and its UI/service behaviour cannot be implemented.
