package com.example.carrental;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
        jdbcTemplate.update("DELETE FROM reservation");
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
}
