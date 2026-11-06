package com.example.carrental;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationEndpointIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @BeforeEach
    void clearsReservations() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS force_reservation_conflict ON reservation");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS force_reservation_conflict()");
        jdbcTemplate.update("DELETE FROM reservation");
    }

    @AfterEach
    void removesConflictTrigger() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS force_reservation_conflict ON reservation");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS force_reservation_conflict()");
    }

    @Test
    void createsReservationAndReturnsItsDetails() throws Exception {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0);

        HttpResponse<String> response = createReservation("SEDAN", start, 2);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.body()).contains("\"carId\":1", "\"carType\":\"SEDAN\"", "\"status\":\"ACTIVE\"");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM reservation", Integer.class)).isEqualTo(1);
    }

    @Test
    void returnsBadRequestForInvalidAndPastRequests() throws Exception {
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0);
        OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).withNano(0);

        assertThat(createReservation("SEDAN", future, 0).statusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(createReservation("SEDAN", past, 1).statusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(postReservation("{\"carType\":\"UNKNOWN\",\"start\":\""
                + future + "\",\"days\":1}").statusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void returnsConflictWhenTheTypeHasNoAvailableCars() throws Exception {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0);

        assertThat(createReservation("VAN", start, 1).statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(createReservation("VAN", start, 1).statusCode()).isEqualTo(HttpStatus.CREATED.value());
        HttpResponse<String> response = createReservation("VAN", start, 1);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM reservation", Integer.class)).isEqualTo(2);
    }

    @Test
    void allowsAdjacentReservationPeriods() throws Exception {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5).withNano(0);

        assertThat(createReservation("SUV", start, 1).statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(createReservation("SUV", start.plusDays(1), 1).statusCode())
                .isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    void concurrentRequestsReserveOnlyTheCarsThatAreAvailable() {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(6).withNano(0);
        var client = HttpClient.newHttpClient();

        List<CompletableFuture<HttpResponse<String>>> responses = IntStream.range(0, 8)
                .mapToObj(index -> createReservationAsync(client, "SEDAN", start, 1))
                .toList();
        CompletableFuture.allOf(responses.toArray(CompletableFuture[]::new)).join();

        List<Integer> statuses = responses.stream()
                .map(CompletableFuture::join)
                .map(HttpResponse::statusCode)
                .toList();
        assertThat(statuses).filteredOn(status -> status == HttpStatus.CREATED.value()).hasSize(2);
        assertThat(statuses).filteredOn(status -> status == HttpStatus.CONFLICT.value()).hasSize(6);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM reservation", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(DISTINCT car_id) FROM reservation", Integer.class)).isEqualTo(2);

        Integer overlappingPairs = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM reservation first
                JOIN reservation second
                  ON first.car_id = second.car_id
                 AND first.id < second.id
                 AND first.starts_at < second.ends_at
                 AND first.ends_at > second.starts_at
                """, Integer.class);
        assertThat(overlappingPairs).isZero();
    }

    @Test
    void returnsConflictAndRollsBackWhenTheDatabaseRejectsAnOverlap() throws Exception {
        jdbcTemplate.execute("""
                CREATE FUNCTION force_reservation_conflict() RETURNS trigger AS $$
                BEGIN
                    IF pg_trigger_depth() = 1 THEN
                        INSERT INTO reservation (car_id, starts_at, ends_at)
                        VALUES (NEW.car_id, NEW.starts_at, NEW.ends_at);
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER force_reservation_conflict
                BEFORE INSERT ON reservation
                FOR EACH ROW EXECUTE FUNCTION force_reservation_conflict()
                """);
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7).withNano(0);

        HttpResponse<String> response = createReservation("SUV", start, 1);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.body()).contains("The requested period is no longer available")
                .doesNotContain("reservation_no_overlapping_periods");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM reservation", Integer.class)).isZero();
    }

    private HttpResponse<String> createReservation(String type, OffsetDateTime start, int days) throws Exception {
        return postReservation("{\"carType\":\"" + type + "\",\"start\":\"" + start
                + "\",\"days\":" + days + "}");
    }

    private HttpResponse<String> postReservation(String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private CompletableFuture<HttpResponse<String>> createReservationAsync(
            HttpClient client, String type, OffsetDateTime start, int days) {
        String body = "{\"carType\":\"" + type + "\",\"start\":\"" + start + "\",\"days\":" + days + "}";
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
