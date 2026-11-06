package com.example.carrental.application.port;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;
import java.util.Optional;

public interface ReservationStorePort {

    Reservation save(Car car, Period period);

    Optional<Reservation> findById(long reservationId);

    /** Call after locking the car type so cancellation uses the same lock order as reservation creation. */
    Optional<Reservation> findByIdForUpdate(long reservationId);

    void update(Reservation reservation);
}
