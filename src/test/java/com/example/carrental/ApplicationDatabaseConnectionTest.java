package com.example.carrental;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationDatabaseConnectionTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.6"))
                    .withInitScript("db/init/001-create-schema.sql");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void setDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void connectsToPostgreSql() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void initializesTwoCarsForEachType() {
        List<String> carTypes = jdbcTemplate.queryForList(
                "SELECT type_code FROM car ORDER BY type_code, id", String.class);

        assertThat(carTypes).containsExactly("SEDAN", "SEDAN", "SUV", "SUV", "VAN", "VAN");
    }

    @Test
    void rejectsAReservationWithAnInvalidPeriod() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO reservation (car_id, starts_at, ends_at)
                VALUES (1, '2026-11-05T10:00:00Z', '2026-11-05T10:00:00Z')
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAReservationForAnUnknownCar() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO reservation (car_id, starts_at, ends_at)
                VALUES (99999, '2026-11-05T10:00:00Z', '2026-11-06T10:00:00Z')
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void healthEndpointReportsAnAvailableDatabase() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"))
                .GET()
                .build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }
}
