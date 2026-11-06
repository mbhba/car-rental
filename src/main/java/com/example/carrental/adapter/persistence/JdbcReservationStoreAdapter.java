package com.example.carrental.adapter.persistence;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.carrental.application.ReservationConflictException;
import com.example.carrental.application.port.ReservationStorePort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;
import com.example.carrental.domain.ReservationStatus;

@Repository
public class JdbcReservationStoreAdapter implements ReservationStorePort {

    private static final String POSTGRES_EXCLUSION_VIOLATION_SQL_STATE = "23P01";

    private final JdbcTemplate jdbcTemplate;

    public JdbcReservationStoreAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Reservation save(Car car, Period period) {
        try {
            Long id = jdbcTemplate.queryForObject("""
                    INSERT INTO reservation (car_id, starts_at, ends_at)
                    VALUES (?, ?, ?)
                    RETURNING id
                    """, (resultSet, rowNumber) -> resultSet.getLong("id"),
                    car.id(), period.start(), period.end());
            return new Reservation(id, car, period);
        } catch (DataAccessException exception) {
            // Translate only exclusion conflicts; other database errors keep the generic server-error path.
            if (hasSqlState(exception, POSTGRES_EXCLUSION_VIOLATION_SQL_STATE)) {
                throw new ReservationConflictException(exception);
            }
            throw exception;
        }
    }

    @Override
    public Optional<Reservation> findById(long reservationId) {
        return findReservation(reservationId, false);
    }

    @Override
    public Optional<Reservation> findByIdForUpdate(long reservationId) {
        return findReservation(reservationId, true);
    }

    @Override
    public void update(Reservation reservation) {
        jdbcTemplate.update("UPDATE reservation SET status = ? WHERE id = ?",
                reservation.status().name(), reservation.id());
    }

    private Optional<Reservation> findReservation(long reservationId, boolean forUpdate) {
        String lockClause = forUpdate ? " FOR UPDATE OF r" : "";
        String sql = """
                SELECT r.id, r.starts_at, r.ends_at, r.status, c.id AS car_id, c.type_code
                FROM reservation r
                JOIN car c ON c.id = r.car_id
                WHERE r.id = ?
                """ + lockClause;
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> {
            OffsetDateTime start = resultSet.getObject("starts_at", OffsetDateTime.class);
            OffsetDateTime end = resultSet.getObject("ends_at", OffsetDateTime.class);
            return new Reservation(
                    resultSet.getLong("id"),
                    new Car(resultSet.getLong("car_id"),
                            CarType.valueOf(resultSet.getString("type_code"))),
                    Period.of(start, end),
                    ReservationStatus.valueOf(resultSet.getString("status")));
        }, reservationId).stream().findFirst();
    }

    private boolean hasSqlState(Throwable exception, String sqlState) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && sqlState.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
