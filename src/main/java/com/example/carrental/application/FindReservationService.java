package com.example.carrental.application;

import com.example.carrental.application.port.ReservationStorePort;
import com.example.carrental.domain.Reservation;

public final class FindReservationService implements FindReservation {

    private final ReservationStorePort reservationStore;

    public FindReservationService(ReservationStorePort reservationStore) {
        this.reservationStore = reservationStore;
    }

    @Override
    public Reservation find(long reservationId) {
        if (reservationId <= 0) {
            throw new InvalidReservationException("Reservation ID must be positive");
        }
        return reservationStore.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }
}
