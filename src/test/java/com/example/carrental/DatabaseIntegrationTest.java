package com.example.carrental;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
}
