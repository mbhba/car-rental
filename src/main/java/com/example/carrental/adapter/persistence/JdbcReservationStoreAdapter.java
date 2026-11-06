package com.example.carrental.adapter.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.carrental.application.port.ReservationStorePort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;

@Repository
public class JdbcReservationStoreAdapter implements ReservationStorePort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReservationStoreAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Reservation save(Car car, Period period) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO reservation (car_id, starts_at, ends_at)
                VALUES (?, ?, ?)
                RETURNING id
                """, (resultSet, rowNumber) -> resultSet.getLong("id"),
                car.id(), period.start(), period.end());
        return new Reservation(id, car, period);
    }
}
