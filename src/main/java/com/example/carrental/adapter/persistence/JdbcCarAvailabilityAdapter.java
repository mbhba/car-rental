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

    private static final String FIND_AVAILABLE_CAR = """
            SELECT c.id, c.type_code
            FROM car c
            WHERE c.type_code = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM reservation r
                  WHERE r.car_id = c.id
                    AND r.status = 'ACTIVE'
                    AND r.starts_at < ?
                    AND r.ends_at > ?
              )
            ORDER BY c.id
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCarAvailabilityAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Car> findAvailableCar(CarType type, Period period) {
        List<Car> cars = jdbcTemplate.query(connection -> {
            var statement = connection.prepareStatement(FIND_AVAILABLE_CAR);
            statement.setString(1, type.name());
            statement.setObject(2, period.end());
            statement.setObject(3, period.start());
            return statement;
        }, (resultSet, rowNumber) -> new Car(
                resultSet.getLong("id"),
                CarType.valueOf(resultSet.getString("type_code"))));
        return cars.stream().findFirst();
    }
}
