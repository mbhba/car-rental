package com.example.carrental.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import com.example.carrental.application.port.CarAvailabilityPort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;

public final class SearchAvailableCarsService implements SearchAvailableCars {

    private final CarAvailabilityPort carAvailability;
    private final RequestedPeriod requestedPeriod;

    public SearchAvailableCarsService(CarAvailabilityPort carAvailability, Clock clock) {
        this.carAvailability = carAvailability;
        this.requestedPeriod = new RequestedPeriod(clock);
    }

    @Override
    public List<Car> search(OffsetDateTime start, Integer days) {
        return carAvailability.findAvailableCars(requestedPeriod.create(start, days));
    }

    @Override
    public List<Car> search(CarType type, OffsetDateTime start, Integer days) {
        if (type == null) {
            throw new InvalidReservationException("Car type filter is invalid");
        }
        return carAvailability.findAvailableCars(type, requestedPeriod.create(start, days));
    }
}
