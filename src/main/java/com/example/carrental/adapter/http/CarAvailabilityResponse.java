package com.example.carrental.adapter.http;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;

public record CarAvailabilityResponse(long carId, CarType carType) {

    public static CarAvailabilityResponse from(Car car) {
        return new CarAvailabilityResponse(car.id(), car.type());
    }
}
