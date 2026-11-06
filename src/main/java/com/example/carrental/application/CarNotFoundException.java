package com.example.carrental.application;

public final class CarNotFoundException extends RuntimeException {

    public CarNotFoundException(long carId) {
        super("Car " + carId + " was not found");
    }
}
