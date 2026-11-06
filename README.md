# Car rental reservation system

## Technology versions

As of November 6, 2026, the project versions remain current. I made no version changes.

- Java 25 is the current LTS release. [Oracle Java roadmap](https://www.oracle.com/europe/java/technologies/java-se-support-roadmap.html)
- Spring Boot 4.1.2 is the latest stable release. [Spring Boot releases](https://github.com/spring-projects/spring-boot/releases) and [system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- Gradle 9.8.2 is the latest stable release. It supports running on Java 25. [Gradle releases](https://gradle.org/releases/) and [Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- Testcontainers 2.0.5 is the selected test version. [Testcontainers releases](https://github.com/testcontainers/testcontainers-java/releases)
- PostgreSQL 18.6 is the latest stable release. PostgreSQL 19 is a release candidate. [PostgreSQL release notes](https://www.postgresql.org/docs/release/)
- The Adminer site lists 6.3.0 as its latest release. The official Docker image still lists 6.2.0 tags, so Compose uses 6.2.0. [Adminer releases](https://www.adminer.org/en/) and [Adminer image tags](https://hub.docker.com/_/adminer)
- The local host reports Docker Compose 5.6.0. The project uses PostgreSQL image 18.6 and Adminer image 6.2.0. [PostgreSQL image](https://hub.docker.com/_/postgres)

## Start the services

You need Docker Engine or Docker Desktop with Docker Compose. You do not need Java or Gradle on the host. The Docker image provides Gradle 9.8.2 and Java 25.

From the project directory, start the application, PostgreSQL, and Adminer:

```sh
docker compose up --build -d --wait
```

The first start downloads the required images and Java dependencies. PostgreSQL runs the initialization script when the named volume is empty. The script creates the tables and adds two cars of each type.

The application starts after PostgreSQL reports that it is ready. The named volume keeps database data after you stop the services. The initialization script does not run again while the volume contains a database.

## Try the application

Open `http://localhost:8080/actuator/health` in a browser. A response with `"status":"UP"` means that the application can connect to PostgreSQL.

Open `http://localhost:8081` to use Adminer. Enter `db` as the server, `car_rental` as the database and user, and `car_rental_local` as the password. These credentials are for local demonstration only.

The local database contains two sedans, two SUVs, and two vans after its first start. Phase 3 also adds constraints for car types, car references, reservation periods, and reservation status. Phase 2 adds domain rules, but it does not add API operations.

To view service output, run:

```sh
docker compose ps
docker compose logs app
```

To stop the services, run:

```sh
docker compose down
```

The command above keeps the database volume. The command `docker compose down -v` deletes the volume and its data.

## Run the tests

The test service runs the unit tests and the PostgreSQL integration tests. It starts a temporary PostgreSQL 18.6 container with Testcontainers 2.0.5. It loads the same initialization script and needs access to the Docker socket.

From the project directory, run:

```sh
docker compose --profile test run --rm test
```

The tests cover the current project scope. All tests must pass after a change.

## Design decisions and known limits

Docker Compose starts the three local services on one network. The application waits for PostgreSQL readiness before it starts. Spring Boot Actuator reports application and database health at `/actuator/health`.

The project uses the Groovy DSL in `build.gradle`. The application uses a Gradle image for local development. Compose mounts the source directory and keeps the Gradle cache in a named volume. This lets you edit Java files without rebuilding the image.

The database stores car types as rows. Phase 5 will lock one type row while the application selects a car and saves a reservation. The reservation table stores timestamps with time zones as instants. The overlap constraint is deferred to Phase 5, when the reservation flow uses the type lock.

The test service alone mounts the Docker socket. Testcontainers uses that socket to start a real PostgreSQL database for tests. The local database credentials and Adminer service support manual testing only.

## Current scope

Phase 2 adds a domain model for car types, cars, periods, and reservations. The domain code does not depend on Spring or SQL. Phase 3 adds the PostgreSQL schema and a six-car sample fleet. Phase 4 adds reservation creation by car type.

The period includes its start and excludes its end. A day-based period uses 24-hour days. The model compares dates by their instant, so different UTC offsets describe the same point in time.

Cars and reservations require positive numeric IDs. The database enforces positive unique IDs. A reservation starts as active, and its domain object can move to the cancelled state.

`POST /reservations` accepts `carType`, `start` with a UTC offset, and a positive `days` value. A day is exactly 24 hours. The application assigns the first available car of that type and returns its reservation details with status `201`.

The API returns `400` for missing or invalid values and past start times. It returns `409` when no car is available. Two requests can select the same car before either request saves. Phase 5 adds a type-row lock and a database constraint to prevent that result.

The solution does not yet add the type lock, overlap constraint, or JaCoCo reports. Later core tasks add these features in the order in `spec.md`.
