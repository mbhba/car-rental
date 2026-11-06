package com.example.carrental.adapter.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.carrental.application.port.CarAvailabilityPort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;

@Repository
public class JdbcCarAvailabilityAdapter implements CarAvailabilityPort {

    private static final String AVAILABLE_CARS = """
            SELECT c.id, c.type_code
            FROM car c
            WHERE NOT EXISTS (
                  SELECT 1
                  FROM reservation r
                  WHERE r.car_id = c.id
                    AND r.status = 'ACTIVE'
                    AND r.starts_at < ?
                    AND r.ends_at > ?
              )
            ORDER BY c.id
            """;
    private static final String AVAILABLE_CARS_BY_TYPE = AVAILABLE_CARS
            .replace("WHERE NOT EXISTS", "WHERE c.type_code = ? AND NOT EXISTS");

    private static final String CAR_BY_ID = "SELECT id, type_code FROM car WHERE id = ?";
    private static final String CAR_AVAILABLE_BY_ID = """
            SELECT c.id, c.type_code
            FROM car c
            WHERE c.id = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM reservation r
                  WHERE r.car_id = c.id
                    AND r.status = 'ACTIVE'
                    AND r.starts_at < ?
                    AND r.ends_at > ?
              )
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCarAvailabilityAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Car> findAvailableCars(Period period) {
        // A search reads current availability. Reservation creation repeats this query after taking the type lock.
        return jdbcTemplate.query(connection -> {
            var statement = connection.prepareStatement(AVAILABLE_CARS);
            statement.setObject(1, period.end());
            statement.setObject(2, period.start());
            return statement;
        }, (resultSet, rowNumber) -> new Car(
                resultSet.getLong("id"),
                CarType.valueOf(resultSet.getString("type_code"))));
    }

    @Override
    public List<Car> findAvailableCars(CarType type, Period period) {
        return jdbcTemplate.query(connection -> {
            var statement = connection.prepareStatement(AVAILABLE_CARS_BY_TYPE);
            statement.setString(1, type.name());
            statement.setObject(2, period.end());
            statement.setObject(3, period.start());
            return statement;
        }, (resultSet, rowNumber) -> new Car(
                resultSet.getLong("id"),
                CarType.valueOf(resultSet.getString("type_code"))));
    }

    @Override
    public Optional<Car> findCarById(long carId) {
        return jdbcTemplate.query(CAR_BY_ID, (resultSet, rowNumber) -> new Car(
                        resultSet.getLong("id"),
                        CarType.valueOf(resultSet.getString("type_code"))), carId)
                .stream().findFirst();
    }

    @Override
    public Optional<Car> findAvailableCar(long carId, Period period) {
        return jdbcTemplate.query(connection -> {
            var statement = connection.prepareStatement(CAR_AVAILABLE_BY_ID);
            statement.setLong(1, carId);
            statement.setObject(2, period.end());
            statement.setObject(3, period.start());
            return statement;
        }, (resultSet, rowNumber) -> new Car(
                resultSet.getLong("id"),
                CarType.valueOf(resultSet.getString("type_code"))))
                .stream().findFirst();
    }
}
