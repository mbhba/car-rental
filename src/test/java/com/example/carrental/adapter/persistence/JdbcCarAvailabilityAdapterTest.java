package com.example.carrental.adapter.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcCarAvailabilityAdapterTest {

    @Test
    void mapsTheFirstAvailableCarAndUsesHalfOpenPeriodBounds() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Period period = Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 2);
        doAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            PreparedStatement statement = mock(PreparedStatement.class);
            Connection connection = mock(Connection.class);
            when(connection.prepareStatement(any(String.class))).thenReturn(statement);
            creator.createPreparedStatement(connection);

            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getLong("id")).thenReturn(4L);
            when(resultSet.getString("type_code")).thenReturn("VAN");
            RowMapper<com.example.carrental.domain.Car> mapper = invocation.getArgument(1);
            List<com.example.carrental.domain.Car> cars = List.of(mapper.mapRow(resultSet, 0));

            verify(statement).setString(1, "VAN");
            verify(statement).setObject(2, period.end());
            verify(statement).setObject(3, period.start());
            return cars;
        }).when(jdbcTemplate).query(any(PreparedStatementCreator.class), any(RowMapper.class));
        JdbcCarAvailabilityAdapter adapter = new JdbcCarAvailabilityAdapter(jdbcTemplate);

        var car = adapter.findAvailableCar(CarType.VAN, period);

        assertThat(car).isPresent();
        assertThat(car.orElseThrow().id()).isEqualTo(4);
        assertThat(car.orElseThrow().type()).isEqualTo(CarType.VAN);
    }

    @Test
    void returnsEmptyWhenTheQueryFindsNoAvailableCar() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
                .thenReturn(List.of());
        JdbcCarAvailabilityAdapter adapter = new JdbcCarAvailabilityAdapter(jdbcTemplate);

        assertThat(adapter.findAvailableCar(
                CarType.SEDAN,
                Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 1)))
                .isEmpty();
    }
}
