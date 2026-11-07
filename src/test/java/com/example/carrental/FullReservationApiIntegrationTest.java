package com.example.carrental;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class FullReservationApiIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @BeforeEach
    void clearsReservations() {
        jdbcTemplate.update("DELETE FROM reservation");
    }

    @Test
    void searchesCurrentAvailabilityAndReservationChecksItAgain() throws Exception {
        OffsetDateTime start = futureStart(10);

        HttpResponse<String> search = available(start, 1, "VAN");

        assertThat(search.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(search.body()).contains("\"carId\":5", "\"carId\":6", "\"carType\":\"VAN\"");
        assertThat(createById(5, start, 1).statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(createById(5, start, 1).statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

        HttpResponse<String> refreshed = available(start, 1, "VAN");
        assertThat(refreshed.body()).doesNotContain("\"carId\":5").contains("\"carId\":6");
    }

    @Test
    void returnsAnEmptyArrayWhenAllCarsOfATypeAreUnavailable() throws Exception {
        OffsetDateTime start = futureStart(11);
        assertThat(createById(1, start, 1).statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(createById(2, start, 1).statusCode()).isEqualTo(HttpStatus.CREATED.value());

        HttpResponse<String> response = available(start, 1, "SEDAN");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).isEqualTo("[]");
    }

    @Test
    void searchesAcrossTypesWhenTheTypeFilterIsMissing() throws Exception {
        HttpResponse<String> response = available(futureStart(11), 1, null);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("\"carType\":\"SEDAN\"", "\"carType\":\"SUV\"", "\"carType\":\"VAN\"");
    }

    @Test
    void reservesByIdAndReturnsNotFoundForAnUnknownCar() throws Exception {
        OffsetDateTime start = futureStart(12);

        assertThat(createById(999, start, 1).statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(createById(3, start, 1).statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(createById(3, start, 1).statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void returnsBadRequestForInvalidSearchAndReservationInputs() throws Exception {
        OffsetDateTime start = futureStart(12);
        OffsetDateTime tooFarAhead = futureStart(366);
        String encodedStart = URLEncoder.encode(start.toString(), StandardCharsets.UTF_8);
        String encodedTooFarAhead = URLEncoder.encode(tooFarAhead.toString(), StandardCharsets.UTF_8);
        assertThat(get("/cars/available").statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(get("/cars/available?start=" + encodedStart + "&days=1&carType=UNKNOWN").statusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(get("/cars/available?start=" + encodedStart + "&days=366").statusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(get("/cars/available?start=" + encodedTooFarAhead + "&days=1").statusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(post("/reservations", reservationJson("\"carType\":\"SUV\",\"carId\":3", start, 1))
                .statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(post("/reservations", reservationJson("\"carId\":0", start, 1))
                .statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void readsCancelsRepeatsCancellationAndReleasesTheCarPeriod() throws Exception {
        OffsetDateTime start = futureStart(13);
        HttpResponse<String> created = createById(4, start, 1);
        long reservationId = Long.parseLong(created.body().replaceFirst(".*\\\"id\\\":(\\d+).*", "$1"));

        HttpResponse<String> found = get("/reservations/" + reservationId);
        assertThat(found.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(found.body()).contains("\"status\":\"ACTIVE\"");
        assertThat(get("/reservations/999999").statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

        HttpResponse<String> cancelled = post("/reservations/" + reservationId + "/cancel", "");
        assertThat(cancelled.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(cancelled.body()).contains("\"status\":\"CANCELLED\"");
        assertThat(post("/reservations/" + reservationId + "/cancel", "").body())
                .contains("\"status\":\"CANCELLED\"");
        assertThat(post("/reservations/999999/cancel", "").statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(createById(4, start, 1).statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM reservation WHERE status = 'CANCELLED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentRequestsForTheSameCarDoNotCreateOverlappingReservations() {
        OffsetDateTime start = futureStart(14);
        HttpClient client = HttpClient.newHttpClient();

        List<CompletableFuture<HttpResponse<String>>> requests = IntStream.range(0, 8)
                .mapToObj(index -> createByIdAsync(client, 6, start, 1))
                .toList();
        CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).join();

        List<Integer> statuses = requests.stream().map(CompletableFuture::join)
                .map(HttpResponse::statusCode).toList();
        assertThat(statuses).filteredOn(status -> status == HttpStatus.CREATED.value()).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == HttpStatus.CONFLICT.value()).hasSize(7);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM reservation WHERE car_id = 6 AND status = 'ACTIVE'", Integer.class))
                .isEqualTo(1);
    }

    private HttpResponse<String> available(OffsetDateTime start, int days, String type) throws Exception {
        String query = "?start=" + URLEncoder.encode(start.toString(), StandardCharsets.UTF_8)
                + "&days=" + days + (type == null ? "" : "&carType=" + type);
        return get("/cars/available" + query);
    }

    private HttpResponse<String> createById(long carId, OffsetDateTime start, int days) throws Exception {
        return post("/reservations", reservationJson("\"carId\":" + carId, start, days));
    }

    private CompletableFuture<HttpResponse<String>> createByIdAsync(
            HttpClient client, long carId, OffsetDateTime start, int days) {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(reservationJson("\"carId\":" + carId, start, days)))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private String reservationJson(String vehicle, OffsetDateTime start, int days) {
        return "{" + vehicle + ",\"start\":\"" + start + "\",\"days\":" + days + "}";
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private OffsetDateTime futureStart(int days) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(days).withNano(0);
    }
}
