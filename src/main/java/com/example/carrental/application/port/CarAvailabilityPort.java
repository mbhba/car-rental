package com.example.carrental.application.port;

import java.util.List;
import java.util.Optional;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;

public interface CarAvailabilityPort {

    List<Car> findAvailableCars(Period period);

    List<Car> findAvailableCars(CarType type, Period period);

    Optional<Car> findCarById(long carId);

    Optional<Car> findAvailableCar(long carId, Period period);
}
