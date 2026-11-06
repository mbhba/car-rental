package com.example.carrental.adapter.persistence;

import org.springframework.transaction.annotation.Transactional;

import com.example.carrental.application.CancelReservation;
import com.example.carrental.domain.Reservation;

/** Keeps the type lock, reservation row lock, and status update in one transaction. */
public class TransactionalCancelReservation implements CancelReservation {

    private final CancelReservation delegate;

    public TransactionalCancelReservation(CancelReservation delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public Reservation cancel(long reservationId) {
        return delegate.cancel(reservationId);
    }
}
