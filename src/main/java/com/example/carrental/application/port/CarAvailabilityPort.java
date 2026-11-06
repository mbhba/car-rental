package com.example.carrental.application.port;

import java.util.Optional;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;

public interface CarAvailabilityPort {

    Optional<Car> findAvailableCar(CarType type, Period period);
}
