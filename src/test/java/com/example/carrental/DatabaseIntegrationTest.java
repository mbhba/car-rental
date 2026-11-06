package com.example.carrental;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearsReservations() {
        jdbcTemplate.update("DELETE FROM reservation");
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
    void rejectsOverlappingReservationsForTheSameCar() {
        jdbcTemplate.update("""
                INSERT INTO reservation (car_id, starts_at, ends_at)
                VALUES (1, '2026-11-10T10:00:00Z', '2026-11-12T10:00:00Z')
                """);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO reservation (car_id, starts_at, ends_at)
                VALUES (1, '2026-11-11T10:00:00Z', '2026-11-13T10:00:00Z')
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM reservation", Integer.class)).isEqualTo(1);
    }

    @Test
    void migrationAddsTheOverlapConstraintAndCanRunAgain() throws IOException {
        jdbcTemplate.execute("ALTER TABLE reservation DROP CONSTRAINT reservation_no_overlapping_periods");
        String migrationSql = new ClassPathResource("db/init/002-protect-overlapping-periods.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        int extensionEnd = migrationSql.indexOf(';');
        String extension = migrationSql.substring(0, extensionEnd + 1);
        String constraintBlock = migrationSql.substring(extensionEnd + 1);

        jdbcTemplate.execute(extension);
        jdbcTemplate.execute(constraintBlock);
        jdbcTemplate.execute(constraintBlock);

        Integer constraintCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_constraint
                WHERE conrelid = 'reservation'::regclass
                  AND conname = 'reservation_no_overlapping_periods'
                """, Integer.class);
        assertThat(constraintCount).isEqualTo(1);
    }
}
