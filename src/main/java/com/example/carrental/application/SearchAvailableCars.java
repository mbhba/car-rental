package com.example.carrental.application;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;

public interface SearchAvailableCars {

    List<Car> search(OffsetDateTime start, Integer days);

    List<Car> search(CarType type, OffsetDateTime start, Integer days);
}
