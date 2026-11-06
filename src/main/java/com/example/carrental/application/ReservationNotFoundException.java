package com.example.carrental.application;

public final class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(long reservationId) {
        super("Reservation " + reservationId + " was not found");
    }
}
