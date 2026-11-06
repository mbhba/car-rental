package com.example.carrental.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.carrental.domain.CarType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcReservationLockAdapterTest {

    @Test
    void locksTheRequestedTypeRow() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                "SELECT code FROM car_type WHERE code = ? FOR UPDATE", String.class, "SUV"))
                .thenReturn("SUV");
        JdbcReservationLockAdapter adapter = new JdbcReservationLockAdapter(jdbcTemplate);

        adapter.lockCarType(CarType.SUV);

        verify(jdbcTemplate).queryForObject(
                eq("SELECT code FROM car_type WHERE code = ? FOR UPDATE"), eq(String.class), eq("SUV"));
    }
}
