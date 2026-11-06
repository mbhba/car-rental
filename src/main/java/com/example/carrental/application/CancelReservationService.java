package com.example.carrental.application;

import com.example.carrental.application.port.ReservationLockPort;
import com.example.carrental.application.port.ReservationStorePort;
import com.example.carrental.domain.Reservation;

public final class CancelReservationService implements CancelReservation {

    private final ReservationStorePort reservationStore;
    private final ReservationLockPort reservationLock;

    public CancelReservationService(ReservationStorePort reservationStore, ReservationLockPort reservationLock) {
        this.reservationStore = reservationStore;
        this.reservationLock = reservationLock;
    }

    @Override
    public Reservation cancel(long reservationId) {
        if (reservationId <= 0) {
            throw new InvalidReservationException("Reservation ID must be positive");
        }
        Reservation existing = reservationStore.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Lock type before locking the reservation row to match the creation lock order and avoid deadlocks.
        reservationLock.lockCarType(existing.car().type());
        Reservation current = reservationStore.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        current.cancel();
        reservationStore.update(current);
        return current;
    }
}
