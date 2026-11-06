package com.example.carrental.adapter.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.carrental.application.port.ReservationLockPort;
import com.example.carrental.domain.CarType;

@Repository
public class JdbcReservationLockAdapter implements ReservationLockPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReservationLockAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void lockCarType(CarType type) {
        // The type row serializes reservations across application instances for the same fleet type.
        jdbcTemplate.queryForObject(
                "SELECT code FROM car_type WHERE code = ? FOR UPDATE",
                String.class,
                type.name());
    }
}
