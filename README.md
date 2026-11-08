# Room Booking — Werkstatt test project

A small meeting room booking application (Java 25, Spring Boot, Vaadin Flow, jOOQ, Flyway, PostgreSQL) that serves
as the test project of the [AI Unified Process Werkstatt](https://github.com/AI-Unified-Process/marketplace/blob/main/docs/werkstatt.md).

`main` holds the specification (`docs/`) and a runnable skeleton without any use case built. The use cases are
written on a change branch, set to `Approved`, and built by the Werkstatt, which opens a pull request against that
change branch.

## Specification

| Document | Content |
|---|---|
| [docs/vision.md](docs/vision.md) | Problem, users, scope |
| [docs/requirements.md](docs/requirements.md) | FR, NFR, constraints |
| [docs/entity_model.md](docs/entity_model.md) | ROOM, BOOKING |
| [docs/use_cases.puml](docs/use_cases.puml) | UC-001 Register Room, UC-002 Book Room |
| [docs/architecture/stack.md](docs/architecture/stack.md) | Layers, layer skills, commands |

## Running the Werkstatt

1. Add the secret `ANTHROPIC_API_KEY` or `CLAUDE_CODE_OAUTH_TOKEN` (Settings → Secrets and variables → Actions).
2. Start a run on the change branch:

   ```bash
   gh workflow run aiup-werkstatt.yml --ref change/room-registration
   ```

3. Review the pull request the Werkstatt opens against the change branch; the run records are the artifact
   `werkstatt-<run id>`.

## Running the application

Docker is needed for the PostgreSQL (Testcontainers) and the jOOQ code generation.

```bash
./mvnw -B -ntp verify          # build and tests
./mvnw spring-boot:test-run    # the application on http://localhost:8080
```
