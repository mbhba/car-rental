# Car rental reservation system

## Technology versions

All pinned versions were stable on November 6, 2026. The project keeps those versions.

- Java 25 is the current long-term support release. [Oracle Java roadmap](https://www.oracle.com/europe/java/technologies/java-se-support-roadmap.html)
- Spring Boot 4.1.2 supports Java 25. [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- Gradle 9.8.2 is the current stable release. It supports Java 25. [Gradle release notes](https://docs.gradle.org/current/release-notes.html) and [Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- Testcontainers 2.0.5 starts PostgreSQL for integration tests. [Testcontainers releases](https://github.com/testcontainers/testcontainers-java/releases)
- JaCoCo 0.8.16 measures production line coverage. [JaCoCo changes](https://www.jacoco.org/jacoco/trunk/doc/changes.html)
- PostgreSQL 18.6 is the current stable release. PostgreSQL 19 is a release candidate. [PostgreSQL release notes](https://www.postgresql.org/docs/release/)
- The Docker Compose file uses Adminer 6.2.0. The official image provides that release. [Adminer image tags](https://hub.docker.com/_/adminer)

## Start the services

You need Docker Engine or Docker Desktop with Docker Compose. You do not need Java or Gradle on the host. The Docker image provides Gradle 9.8.2 and Java 25.

From the project directory, start the application, PostgreSQL, and Adminer:

```sh
docker compose up --build -d --wait
```

The first start downloads the required images and Java dependencies. PostgreSQL runs the SQL scripts when the named volume is empty. The scripts create the tables, constraints, and six sample cars.

The application starts after PostgreSQL reports that it is ready. The named volume keeps database data after you stop the services. PostgreSQL does not run initialization scripts again while the volume contains a database.

If the database volume does not use the active-only overlap constraint, run this migration from the project directory:

```sh
docker compose exec -T db psql -U car_rental -d car_rental < src/main/resources/db/init/003-active-reservations-only.sql
```

The migration replaces the overlap constraint. It lets a new active reservation reuse a cancelled period.

If active reservations overlap for one car, resolve those rows before you run the migration.

Open `http://localhost:8080/actuator/health` in a browser. A response with `"status":"UP"` means that the application can connect to PostgreSQL.

Open `http://localhost:8081` to use Adminer. Enter `db` as the server, `car_rental` as the database and user, and `car_rental_local` as the password. These credentials support local use only.

To view service output, run:

```sh
docker compose ps
docker compose logs app
```

To stop the services, run:

```sh
docker compose down
```

This command keeps the database volume. The command `docker compose down -v` deletes the volume and its data.

## REST API

The API uses JSON for reservation requests and responses. A period includes its start and excludes its end. Each day equals exactly 24 hours. A UTC offset shows the difference from UTC.

The `start` value uses an ISO 8601 date and time with a UTC offset. It cannot be in the past. It can be no more than 365 days ahead. Exactly 365 days ahead is allowed.

The request can cover 1 to 365 days. These limits apply to searches and reservations. The API rejects requests outside these limits before database access.

Use a future start value when you send the examples below.

### Search for available cars

Send `GET /cars/available` with the required `start` and `days` query parameters. Add the optional `carType` parameter to filter by type.

```sh
curl 'http://localhost:8080/cars/available?start=2026-12-10T10%3A00%3A00Z&days=2&carType=SEDAN'
```

The response uses HTTP status `200`. Each item contains a `carId` and a `carType`:

```json
[
  {"carId": 1, "carType": "SEDAN"},
  {"carId": 2, "carType": "SEDAN"}
]
```

The API returns an empty array when no car matches. A search reports availability at the time of the read. It does not hold a car. A reservation request makes sure that a car remains free before it saves.

The API returns `400` for missing or invalid values, unknown car types, and past start times. It also returns `400` for starts more than 365 days ahead or periods longer than 365 days.

### Create a reservation

Send `POST /reservations` with a JSON body. Provide exactly one of `carType` or `carId`.

To reserve the first available car of a type, send:

```json
{
  "carType": "SEDAN",
  "start": "2026-12-10T10:00:00+02:00",
  "days": 2
}
```

To reserve a specific car, send:

```json
{
  "carId": 1,
  "start": "2026-12-10T10:00:00+02:00",
  "days": 2
}
```

The API returns HTTP status `201` and the saved reservation:

```json
{
  "id": 12,
  "carId": 1,
  "carType": "SEDAN",
  "start": "2026-12-10T10:00:00+02:00",
  "end": "2026-12-12T10:00:00+02:00",
  "status": "ACTIVE"
}
```

The API returns `400` when both car fields appear, when neither field appears, or when a value is invalid. It also returns `400` when the period exceeds 365 days or the start is more than 365 days ahead. It returns `404` when `carId` does not exist. It returns `409` when the requested car or type has no available car. It returns `500` for an unexpected database error. Error responses contain a `message` field and do not expose database details.

### Read a reservation

Send `GET /reservations/{id}` to read one reservation. The response uses HTTP status `200` and the same fields as the creation response.

The response includes the current status, `ACTIVE` or `CANCELLED`. The API returns `404` for an unknown reservation ID. It returns `400` for a non-positive or invalid ID.

### Cancel a reservation

Send `POST /reservations/{id}/cancel` to cancel a reservation. The response uses HTTP status `200` and returns the current reservation state.

The API returns `404` for an unknown reservation ID. It returns `400` for a non-positive or invalid ID. A repeated cancellation returns the same `CANCELLED` state. Cancellation releases the car period for another reservation.

## Run the tests

The test service runs unit tests, PostgreSQL integration tests, and the JaCoCo coverage rule. It starts a temporary PostgreSQL 18.6 container with Testcontainers 2.0.5. The service needs access to the Docker socket.

Gradle `test` runs unit tests. Gradle `integrationTest` runs tests against PostgreSQL. Gradle `check` runs both tasks and the JaCoCo coverage rule.

JaCoCo measures production line coverage from unit tests. The build fails when coverage is below 80%. The target is 90%. The HTML report is at `build/reports/jacoco/test/html/index.html`.

From the project directory, run:

```sh
docker compose --profile test run --rm test
```

The tests cover availability search, reservation by type and ID, reservation lookup, cancellation, period boundaries, database constraints, and concurrent requests. They also make sure that an earlier availability result does not guarantee a later reservation.

## Design decisions and known limits

The application uses ports and adapters. Application services depend on interfaces. HTTP controllers and PostgreSQL adapters implement the outer boundaries. Domain objects enforce valid periods and reservation state without depending on Spring or SQL.

Each reservation transaction locks its car type row, makes sure that a car is free, and saves a reservation. PostgreSQL holds this row lock across application instances. Requests for the same type wait. Requests for different types can proceed at the same time. This choice keeps car selection simple, but it limits parallel reservations for one type.

For a reservation by car ID, the application reads the car type before it locks that type row. Cancellation reads the car type, locks the type row, then locks the reservation row. This order keeps creation and cancellation from taking locks in opposite order.

PostgreSQL stores reservation times as instants. The exclusion constraint rejects overlapping active periods for one car. It allows adjacent periods because each period includes its start and excludes its end. The constraint protects writes that bypass the application lock.

Availability search does not lock the type row because it does not hold a car. The reservation operation repeats the availability query after it locks the type row. A car can become busy between a search and a reservation.

The application injects a clock so time rules use a stable source in unit tests. PostgreSQL stores cancelled rows for history, but those rows do not block active reservations.

The project omits customers, payments, authentication, and fleet management. The specification excludes these features from this recruitment project. Adminer and its credentials support local demonstration only.

The application uses a small sample fleet with two cars of each type. The type row lock limits parallel reservation requests for one type. The specification lists finer-grained locking as a separate future improvement.
