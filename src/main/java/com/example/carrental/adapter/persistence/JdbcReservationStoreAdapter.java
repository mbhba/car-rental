package com.example.carrental.adapter.persistence;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.carrental.application.ReservationConflictException;
import com.example.carrental.application.port.ReservationStorePort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;

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

    private boolean hasSqlState(Throwable exception, String sqlState) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && sqlState.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
