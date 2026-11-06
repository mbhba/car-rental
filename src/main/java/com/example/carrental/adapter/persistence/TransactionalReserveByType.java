package com.example.carrental.adapter.persistence;

import java.time.OffsetDateTime;

import org.springframework.transaction.annotation.Transactional;

import com.example.carrental.application.ReserveByType;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;

/** Starts the database transaction before the use case locks a type and selects a car. */
public class TransactionalReserveByType implements ReserveByType {

    private final ReserveByType delegate;

    public TransactionalReserveByType(ReserveByType delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public Reservation reserve(CarType type, OffsetDateTime start, Integer days) {
        return delegate.reserve(type, start, days);
    }
}
