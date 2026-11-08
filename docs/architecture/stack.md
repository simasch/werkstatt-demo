# Stack

**For the agent:** read before writing code. This file is the AI Unified Process record of the technical layers,
independent of how the team documents its architecture. The Layers table says which layer skill builds and tests
each technical layer: `/use-case-implement` runs the Skill column bottom-up, `/use-case-test` runs the Test skill
column from the integration layer up, and `/use-case-coverage` searches for the markers in the last table.

**Last updated:** 2026-11-08

## Layers

| Layer       | Technology  | Skill            | Test skill            | Location                               |
|-------------|-------------|------------------|-----------------------|----------------------------------------|
| Migration   | Flyway      | flyway-schema    | –                     | `src/main/resources/db/migration/`     |
| Persistence | jOOQ        | jooq-persistence | jooq-persistence-test | `ai.unifiedprocess.roombooking.<feature>.domain` |
| Client      | –           | –                | –                     | –                                      |
| Service     | Spring Boot | spring-service   | spring-service-test   | `ai.unifiedprocess.roombooking.<feature>.service` |
| API         | –           | –                | –                     | –                                      |
| UI          | Vaadin Flow | vaadin-flow-ui   | vaadin-flow-ui-test   | `ai.unifiedprocess.roombooking.<feature>.ui` |
| E2E         | –           | –                | –                     | –                                      |

**Integration layer:**

| Use case offered through | Tested in | Test skill            |
|--------------------------|-----------|-----------------------|
| UI                       | UI        | `vaadin-flow-ui-test` |
| Service                  | Service   | `spring-service-test` |

A layer the project does not have is marked `–` in every column (a UI layer in a REST service). The Integration
layer table keeps only the rows of the entry layers the project has — a REST service drops the UI row — and its
Test skill column names what the Layers table names for that layer (`–` while the skill is not installed). A use
case is tested in the first row its `**Layers:**` line names, or in the first row when it has no such line. That layer's tests run in process against the real database
and exercise every layer below it, so `/use-case-test` runs its test skill and the ones above it and writes no tests
of their own for the layers below unless a run names them (`/use-case-test UC-001 persistence`).

## Module layout

Flat single Maven module, package by feature under the root package `ai.unifiedprocess.roombooking`. Each feature
(`room`, `booking`) has three sub-packages: `domain` (records and the jOOQ `<Aggregate>Repository`), `service` (the
`@Service` classes and domain exceptions), and `ui` (Vaadin views and forms). `core/ui/` holds the application shell
`MainLayout`; a view annotated with `@Menu` appears in its menu. Generated jOOQ sources live in
`ai.unifiedprocess.roombooking.database` under `target/generated-sources/jooq/` and are never edited. Tests mirror
the main packages under `src/test/java/`, import `TestcontainersConfiguration` for the PostgreSQL, and keep test
data in `src/test/resources/db/migration/`.

## Commands

| Purpose            | Command |
|--------------------|---------|
| Build              | `./mvnw -B -ntp verify` |
| Unit tests         | `./mvnw -B -ntp test` |
| Integration tests  | `./mvnw -B -ntp test` (Browserless and service tests against Testcontainers) |
| End-to-end tests   | – |

## Markers

Each layer skill keeps its own rows here: on every run it checks that the rows for its layer match its marker
reference and corrects them. Do not edit the rows by hand; rerun the layer skill instead.

| Layer       | Skill                 | Kind | Marker to grep for |
|-------------|-----------------------|------|--------------------|
| All         | –                     | Code | `// UC-XXX BR-YYY:` directly above the method, query condition, or validator that enforces the rule |
| Migration   | flyway-schema         | Code | `-- UC-XXX BR-YYY:` directly above the `CHECK`, `UNIQUE`, index, or trigger that enforces the rule |
| Migration   | flyway-schema         | Name | `<location>/V*__*.sql`, named after the tables they change, never after a use case — search them by table and column |
| Persistence | jooq-persistence      | Code | `// UC-XXX BR-YYY:` directly above the jOOQ `.where(...)`/`.and(...)` condition or query that enforces the rule |
| Persistence | jooq-persistence      | Name | `<Aggregate>Repository` (`@Repository` with a `DSLContext`, or the adapter implementing that outbound port in a hexagonal layout), named after the aggregate root, never a use case |
| Persistence | jooq-persistence-test | Test | `@UseCase(id = "UC-XXX", scenario = "…", businessRules = {"BR-YYY"}, requirements = {"NFR-XXX"})` on every repository test method |
| Persistence | jooq-persistence-test | Name | `UC<id><Name>RepositoryTest`, e.g. `UC001RegisterPersonRepositoryTest`; test data in `src/test/resources/db/migration/R__uc<id>_<slug>.sql` |
| Service     | spring-service        | Code | `// UC-XXX BR-YYY:` directly above the service method or guard clause that enforces the rule |
| Service     | spring-service        | Code | `UC-XXX` in the Javadoc of the public service method that realizes a step or an alternative flow |
| Service     | spring-service        | Name | `<Entity>Service` classes annotated `@Service`; domain exceptions `<Reason>Exception` |
| Service     | spring-service-test   | Test | `@UseCase(id = "UC-XXX", scenario = "…", businessRules = {"BR-YYY"}, requirements = {"NFR-XXX"})` on every service test method |
| Service     | spring-service-test   | Name | `UC<id><Name>ServiceTest`, e.g. `UC001RegisterGuestServiceTest` |
| UI          | vaadin-flow-ui        | Code | `UC-XXX` in the view's class Javadoc (`/** UC-XXX <Use Case Name>. */`) |
| UI          | vaadin-flow-ui        | Code | `// UC-XXX BR-YYY:` directly above the `Binder` validator or UI guard that enforces the rule |
| UI          | vaadin-flow-ui        | Name | `@Route` view class `<Name>View` named after the use case (`ManagePersonsView` for "Manage Persons") |
| UI          | vaadin-flow-ui-test   | Test | `@UseCase(id = "UC-XXX", scenario = "…", businessRules = {"BR-YYY"}, requirements = {"NFR-XXX"})` on every Browserless test method |
| UI          | vaadin-flow-ui-test   | Name | `UC<id><Name>Test`, e.g. `UC001ManagePersonsTest`, extending `SpringBrowserlessTest` or the project's subclass of it |
