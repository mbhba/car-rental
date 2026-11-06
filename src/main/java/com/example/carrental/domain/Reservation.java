package com.example.carrental.domain;

import java.util.Objects;

public final class Reservation {

    private final long id;
    private final Car car;
    private final Period period;
    private ReservationStatus status;

    public Reservation(long id, Car car, Period period) {
        this(id, car, period, ReservationStatus.ACTIVE);
    }

    public Reservation(long id, Car car, Period period, ReservationStatus status) {
        if (id <= 0) {
            throw new IllegalArgumentException("Reservation ID must be positive");
        }
        this.id = id;
        this.car = Objects.requireNonNull(car, "Reservation car is required");
        this.period = Objects.requireNonNull(period, "Reservation period is required");
        this.status = Objects.requireNonNull(status, "Reservation status is required");
    }

    public long id() {
        return id;
    }

    public Car car() {
        return car;
    }

    public Period period() {
        return period;
    }

    public ReservationStatus status() {
        return status;
    }

    public void cancel() {
        status = ReservationStatus.CANCELLED;
    }
}
