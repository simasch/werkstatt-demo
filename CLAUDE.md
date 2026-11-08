# CLAUDE.md

Room Booking is built spec-first with the AI Unified Process. `docs/` is the source of truth, not the code:

- `docs/requirements.md`, `docs/entity_model.md`, `docs/use_cases.puml`, and `docs/use_cases/UC-*.md` say what to
  build. If a use case and the code disagree, the use case wins.
- `docs/architecture/stack.md` says how: the layers, the layer skill of each, the module layout, and the commands.
  Construction goes through `/use-case-implement` and `/use-case-test`.
- A gap in the specification is recorded in `docs/open_questions.md` (`/open-questions`), never guessed.

Build and test with `./mvnw -B -ntp verify` (Docker required).
