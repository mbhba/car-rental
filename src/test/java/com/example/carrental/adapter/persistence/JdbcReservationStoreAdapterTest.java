package com.example.carrental.adapter.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.example.carrental.application.ReservationConflictException;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcReservationStoreAdapterTest {

    @Test
    void savesTheCarAndPeriodAndBuildsAnActiveReservation() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doAnswer(invocation -> {
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getLong("id")).thenReturn(29L);
            RowMapper<Long> mapper = invocation.getArgument(1);
            return mapper.mapRow(resultSet, 0);
        }).when(jdbcTemplate).queryForObject(
                anyString(), any(RowMapper.class), any(), any(), any());
        JdbcReservationStoreAdapter adapter = new JdbcReservationStoreAdapter(jdbcTemplate);
        Car car = new Car(5, CarType.SUV);
        Period period = Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 1);

        var reservation = adapter.save(car, period);

        assertThat(reservation.id()).isEqualTo(29);
        assertThat(reservation.car()).isSameAs(car);
        assertThat(reservation.period()).isSameAs(period);
        assertThat(reservation.status().name()).isEqualTo("ACTIVE");
    }

    @Test
    void translatesOnlyPostgreSqlOverlapErrors() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        var overlapError = new UncategorizedSQLException(
                "save reservation", "INSERT", new SQLException("overlap", "23P01"));
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(), any(), any()))
                .thenThrow(overlapError);
        JdbcReservationStoreAdapter adapter = new JdbcReservationStoreAdapter(jdbcTemplate);

        assertThatThrownBy(() -> adapter.save(
                new Car(5, CarType.SUV),
                Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 1)))
                .isInstanceOf(ReservationConflictException.class)
                .hasCause(overlapError);
    }

    @Test
    void preservesOtherDatabaseErrors() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        var foreignKeyError = new UncategorizedSQLException(
                "save reservation", "INSERT", new SQLException("foreign key", "23503"));
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(), any(), any()))
                .thenThrow(foreignKeyError);
        JdbcReservationStoreAdapter adapter = new JdbcReservationStoreAdapter(jdbcTemplate);

        assertThatThrownBy(() -> adapter.save(
                new Car(5, CarType.SUV),
                Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 1)))
                .isSameAs(foreignKeyError);
    }
}
