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

import com.example.carrental.domain.Car;
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
    void searchesAllCarsAndMapsTheirIdsAndTypes() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Period period = Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 2);
        doAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            PreparedStatement statement = mock(PreparedStatement.class);
            Connection connection = mock(Connection.class);
            when(connection.prepareStatement(any(String.class))).thenReturn(statement);
            creator.createPreparedStatement(connection);
            verify(statement).setObject(1, period.end());
            verify(statement).setObject(2, period.start());

            ResultSet first = mock(ResultSet.class);
            when(first.getLong("id")).thenReturn(1L);
            when(first.getString("type_code")).thenReturn("SEDAN");
            ResultSet second = mock(ResultSet.class);
            when(second.getLong("id")).thenReturn(4L);
            when(second.getString("type_code")).thenReturn("VAN");
            RowMapper<Car> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(first, 0), mapper.mapRow(second, 1));
        }).when(jdbcTemplate).query(any(PreparedStatementCreator.class), any(RowMapper.class));
        JdbcCarAvailabilityAdapter adapter = new JdbcCarAvailabilityAdapter(jdbcTemplate);

        List<Car> cars = adapter.findAvailableCars(period);

        assertThat(cars).extracting(Car::id).containsExactly(1L, 4L);
        assertThat(cars).extracting(Car::type).containsExactly(CarType.SEDAN, CarType.VAN);
    }

    @Test
    void filtersAvailabilityByTypeAndReturnsAnEmptyListWhenNoneMatch() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Period period = Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 1);
        doAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            PreparedStatement statement = mock(PreparedStatement.class);
            Connection connection = mock(Connection.class);
            when(connection.prepareStatement(any(String.class))).thenReturn(statement);
            creator.createPreparedStatement(connection);
            verify(statement).setString(1, "SUV");
            verify(statement).setObject(2, period.end());
            verify(statement).setObject(3, period.start());
            return List.of();
        }).when(jdbcTemplate).query(any(PreparedStatementCreator.class), any(RowMapper.class));
        JdbcCarAvailabilityAdapter adapter = new JdbcCarAvailabilityAdapter(jdbcTemplate);

        assertThat(adapter.findAvailableCars(CarType.SUV, period)).isEmpty();
    }

    @Test
    void mapsCarLookupAndAvailabilityById() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(org.mockito.ArgumentMatchers.anyString(), any(RowMapper.class),
                org.mockito.ArgumentMatchers.eq(7L)))
                .thenAnswer(invocation -> {
                    RowMapper<Car> mapper = invocation.getArgument(1);
                    ResultSet resultSet = mock(ResultSet.class);
                    when(resultSet.getLong("id")).thenReturn(7L);
                    when(resultSet.getString("type_code")).thenReturn("VAN");
                    return List.of(mapper.mapRow(resultSet, 0));
                });
        Period period = Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 1);
        doAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            PreparedStatement statement = mock(PreparedStatement.class);
            Connection connection = mock(Connection.class);
            when(connection.prepareStatement(any(String.class))).thenReturn(statement);
            creator.createPreparedStatement(connection);
            verify(statement).setLong(1, 7L);
            verify(statement).setObject(2, period.end());
            verify(statement).setObject(3, period.start());
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getLong("id")).thenReturn(7L);
            when(resultSet.getString("type_code")).thenReturn("VAN");
            RowMapper<Car> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        }).when(jdbcTemplate).query(any(PreparedStatementCreator.class), any(RowMapper.class));
        JdbcCarAvailabilityAdapter adapter = new JdbcCarAvailabilityAdapter(jdbcTemplate);

        assertThat(adapter.findCarById(7)).get().extracting(Car::id).isEqualTo(7L);
        assertThat(adapter.findAvailableCar(7, period)).get().extracting(Car::type).isEqualTo(CarType.VAN);
    }
}
