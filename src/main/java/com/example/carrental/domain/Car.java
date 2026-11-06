package com.example.carrental.domain;

import java.util.Objects;

public final class Car {

    private final long id;
    private final CarType type;

    public Car(long id, CarType type) {
        if (id <= 0) {
            throw new IllegalArgumentException("Car ID must be positive");
        }
        this.id = id;
        this.type = Objects.requireNonNull(type, "Car type is required");
    }

    public long id() {
        return id;
    }

    public CarType type() {
        return type;
    }
}
