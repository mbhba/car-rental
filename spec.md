# Car Rental Reservation System Specification

## Goal and Technology

The system lets users find available cars and create, read, and cancel reservations. Correct behavior during concurrent requests and clear safeguards are key goals. The scope fits a recruitment project.

This document describes the target scope. The working core solution supports reservations by car type. Availability search, reservations by car ID, reading reservations, and cancellation are nice-to-have tasks. Their requirements apply after implementation. Before cancellation is implemented, the database constraint covers all reservations.

The application uses Java 25 LTS, Spring Boot 4.1.2, and PostgreSQL. Spring Boot handles HTTP, transactions, and tests. PostgreSQL can reject overlapping reservations. The Java and Spring Boot versions follow the Oracle roadmap (https://www.oracle.com/europe/java/technologies/java-se-support-roadmap.html) and Spring Boot requirements (https://docs.spring.io/spring-boot/system-requirements.html).

Before implementation, check the current stable technology versions again. Record the result and any version change in the README.

## Domain Model and API

The model has car types `SEDAN`, `SUV`, and `VAN`, cars with unique IDs, and reservations. Each reservation refers to one car and has the status `ACTIVE` or `CANCELLED`.

The start date and time include a time zone offset. One day is exactly 24 hours. A period includes its start and excludes its end. A new reservation can start when the previous one ends.

The target API provides these operations:

- `GET /cars/available` accepts a start date and time, a number of days, and an optional car type. It returns available car IDs and types. An empty list means that no car is available.
- `POST /reservations` accepts a start date and time, a number of days, and exactly one of car type or car ID. For a car type, the system assigns an available car. A successful response has status `201` and reservation details.
- `GET /reservations/{id}` returns reservation details and status, or `404`.
- `POST /reservations/{id}/cancel` cancels a reservation. A repeat request returns its current state. An unknown ID returns `404`.

An availability result describes the state at the time of the read. It does not hold a car. The reservation request checks availability again before it saves.

Invalid data, past dates, and a request with both type and ID return `400`. An unknown car ID returns `404`. No available car, or a busy requested car, returns `409`. An unexpected database error returns `500` without internal details.

## Architecture, Transactions, and Documentation

Use ports and adapters. The reservation logic exposes operations through interfaces. Adapters handle HTTP and PostgreSQL. Domain objects protect valid periods and status changes. The domain does not depend on Spring or SQL.

When the application creates a reservation, it starts a transaction and locks the row for the car type with `SELECT ... FOR UPDATE`. For a reservation by car ID, it first finds that car's type. It then checks the requested car or finds an available car of that type. It saves the reservation and commits the transaction. Requests for the same type wait, including requests handled by different application instances. Cancellation locks the type row in the same order before it changes the status.

The database provides a separate safeguard. An `EXCLUDE` constraint with the `btree_gist` extension rejects overlapping periods for the same car. A foreign key, required fields, and a valid-period constraint protect other basic rules. PostgreSQL documents range types and constraints here: https://www.postgresql.org/docs/current/rangetypes.html.

The database constraint also protects against writes that bypass the application. A conflict rolls back the transaction, and the API returns `409`. A type lock simplifies the car selection and the correctness rules. It also limits parallel reservations for the same type. This trade-off fits a small fleet.

Document each important design decision in the code near the relevant logic. Explain the reason and trade-off. Cover interface boundaries, transactions, lock order, and database constraints.

Add a code comment for each deliberate simplification or omitted feature. Explain what the system omits and why. List these decisions and omissions in the README for discussion.

## Run and Test

Docker Compose starts the Java application, PostgreSQL, and Adminer. An SQL script creates the schema, constraints, and sample fleet. The fleet has two cars of each type. A volume stores database data. The README explains that the initialization script runs when the volume is empty.

Unit tests check period rules, car selection, cancellation, and error handling. JaCoCo measures production code coverage from unit tests. The minimum is 80%. The target is 90%.

Integration tests use Testcontainers and real PostgreSQL. They cover availability search, reservations by type and ID, reads, cancellation, unavailable cars, and adjacent periods. They also check that a car from an earlier availability result can become busy before the reservation request.

A concurrency test sends more reservation requests than available cars. It checks the number of successful requests and confirms that no saved reservations overlap. It also tests concurrent requests for the same car ID. A separate test writes directly to PostgreSQL to check the database safeguard without the application.

## Assumptions

The system does not include payments, customers, authentication, or fleet management. Adminer and its credentials support local demonstration only. A reservation does not release a car before its end unless someone cancels it. The application injects a clock so time tests are repeatable.

## Task List

Complete the tasks in order. Run the relevant tests after each task. Core tasks form the working solution. Keep unfinished nice-to-have tasks in this specification for discussion.

### 1. Prepare the Project: Core (Complete)

Scope: Create a Java 25 LTS and Spring Boot 4.1.2 application. Use Docker Compose for the application, PostgreSQL, and Adminer. Add run instructions to the README.

Done when: The services start by following the README. A basic application and database connection check passes.

### 2. Build the Domain Model: Core (Complete)

Scope: Add the `SEDAN`, `SUV`, and `VAN` types, a car, a reservation for one car, and a period object. The period includes its start and excludes its end. One day is exactly 24 hours.

Done when: Domain objects protect valid state and period rules. The related behavior tests pass.

### 3. Prepare the Database: Core (Complete)

Scope: Add an SQL script that creates the schema, constraints, and two cars of each type. Store data in a volume. Explain empty-volume initialization in the README.

Done when: A new database has the required schema and fleet. Data remains after service restart.

### 4. Reserve by Type: Core

Scope: Add `POST /reservations`. It accepts a car type, a date and time with a time zone offset, and a number of days. Success returns `201` and reservation details. Invalid data and past dates return `400`. No availability returns `409`.

Done when: The endpoint saves valid requests and returns the required responses for valid requests and described errors.

### 5. Protect Concurrent Reservations: Core

Scope: A transaction locks the type row with `SELECT ... FOR UPDATE`, checks availability, and saves the reservation. An `EXCLUDE` constraint with `btree_gist` rejects overlapping periods for one car. A constraint conflict returns `409` after the transaction rolls back.

Done when: Concurrent requests cannot save overlapping reservations. A constraint conflict rolls back the transaction and returns the required response.

### 6. Add Unit Tests: Core

Scope: Test period rules, validation, car selection, and no availability. Use an injected clock for time tests. Use JaCoCo to measure production code coverage from unit tests.

Done when: Tests pass and coverage is at least 80%. The target is 90%.

### 7. Add Integration Tests: Core

Scope: Use Testcontainers and real PostgreSQL. Test saving, no availability, adjacent periods, and more concurrent requests than available cars. Use a separate direct SQL test for the database constraint.

Done when: Tests pass. The number of successful reservations matches the available fleet. The database has no overlapping reservations for one car.

### 8. Document the Solution: Core

Scope: Add a sample request, test instructions, design decisions, and known limits to the README. Explain how the type lock works across application instances and how it affects parallel requests.

Done when: A reader can run and test the solution and can explain its trade-offs and omitted features.

### 9. Search Availability: Nice to Have

Scope: Add `GET /cars/available` with a period and an optional car type. The read does not hold a car. The reservation request checks availability again before saving.

Done when: The endpoint returns the correct list for a period and type. Tests cover an empty result and a change in availability before the reservation.

### 10. Reserve by ID: Nice to Have

Scope: Extend `POST /reservations` to accept exactly one of car type or car ID. An unknown ID returns `404`. A busy car returns `409`.

Done when: Validation and responses match the specification. A concurrent reservation test for the same ID prevents overlapping periods.

### 11. Read and Cancel Reservations: Nice to Have

Scope: Add `GET /reservations/{id}`, `POST /reservations/{id}/cancel`, and the `ACTIVE` and `CANCELLED` statuses. A repeat cancellation returns the current state. Cancellation uses the same lock order. The database constraint covers active reservations.

Done when: Reads and cancellations return the specified responses. Repeated cancellation is safe. A cancelled reservation no longer holds its period.

### 12. Extend the Tests: Nice to Have

Scope: Test new operations, changes in availability between read and save, and more boundary cases.

Done when: Tests for new behavior pass. Useful unit tests bring coverage closer to the 90% target.

### 13. Increase Parallel Requests: Nice to Have

Scope: Describe alternatives to locking a whole car type and their trade-offs. Treat implementation as a separate improvement.

Done when: The documentation explains possible approaches, their effects on correctness and parallel requests, and the reason for the current choice.
