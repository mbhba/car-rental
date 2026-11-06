package com.example.carrental.adapter.persistence;

import java.time.OffsetDateTime;

import org.springframework.transaction.annotation.Transactional;

import com.example.carrental.application.ReserveCar;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;

/** Starts a transaction before the use case locks a type and checks availability. */
public class TransactionalReserveCar implements ReserveCar {

    private final ReserveCar delegate;

    public TransactionalReserveCar(ReserveCar delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public Reservation reserve(CarType type, Long carId, OffsetDateTime start, Integer days) {
        return delegate.reserve(type, carId, start, days);
    }
}
