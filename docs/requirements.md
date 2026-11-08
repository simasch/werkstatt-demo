# Requirements

Requirements catalog of Room Booking, derived from [`vision.md`](vision.md). The **Use Cases** column mirrors the
`**Requirements:**` line of every use case in [`use_cases/`](use_cases/).

## Functional Requirements

| ID     | Title               | User Story                                                                                                                         | Use Cases | Priority | Status |
|--------|---------------------|------------------------------------------------------------------------------------------------------------------------------------|-----------|----------|--------|
| FR-001 | Register Room       | As an office manager, I want to register a meeting room with its name, capacity, and equipment so that employees can book it.      | UC-001    | High     | Open   |
| FR-002 | Distinct Room Names | As an office manager, I want a room name rejected when another room already has it so that nobody goes to the wrong room.         | UC-001    | High     | Open   |
| FR-003 | Room Overview       | As an office manager, I want to see every room with its capacity and equipment so that I know what the office offers.              | UC-001    | Medium   | Open   |
| FR-004 | Book Room           | As an employee, I want to book a room for a time slot so that I have a place for my meeting.                                       | UC-002    | High     | Open   |
| FR-005 | No Double Booking   | As an employee, I want a booking rejected when it overlaps another booking of the same room so that two meetings never collide.    | UC-002    | High     | Open   |

## Non-Functional Requirements

| ID      | Title                  | Requirement                                                                                                  | Category  | Use Cases      | Priority | Status |
|---------|------------------------|--------------------------------------------------------------------------------------------------------------|-----------|----------------|----------|--------|
| NFR-001 | Field-Level Correction | Rejected input must be reported at the offending field, with every other entry of the form kept.             | Usability | UC-001, UC-002 | High     | Open   |

## Constraints

| ID    | Title            | Constraint                                                                                                       | Category  | Use Cases | Priority |
|-------|------------------|------------------------------------------------------------------------------------------------------------------|-----------|-----------|----------|
| C-001 | Technology Stack | The application must be built with Java 25, Spring Boot, Vaadin Flow, jOOQ, Flyway, and PostgreSQL.              | Technical | —         | High     |
