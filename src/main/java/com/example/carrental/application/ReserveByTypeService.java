package com.example.carrental.application;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import com.example.carrental.application.port.CarAvailabilityPort;
import com.example.carrental.application.port.ReservationLockPort;
import com.example.carrental.application.port.ReservationStorePort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;

public final class ReserveByTypeService implements ReserveByType {

    private final CarAvailabilityPort carAvailability;
    private final ReservationLockPort reservationLock;
    private final ReservationStorePort reservationStore;
    private final Clock clock;

    public ReserveByTypeService(
            CarAvailabilityPort carAvailability,
            ReservationLockPort reservationLock,
            ReservationStorePort reservationStore,
            Clock clock) {
        this.carAvailability = carAvailability;
        this.reservationLock = reservationLock;
        this.reservationStore = reservationStore;
        this.clock = clock;
    }

    @Override
    public Reservation reserve(CarType type, OffsetDateTime start, Integer days) {
        if (type == null || start == null || days == null) {
            throw new InvalidReservationException("Car type, start, and days are required");
        }

        Period period;
        try {
            period = Period.forDays(start, days);
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new InvalidReservationException("The reservation period is invalid", exception);
        }

        if (period.start().toInstant().isBefore(clock.instant())) {
            throw new InvalidReservationException("The reservation start must not be in the past");
        }

        // Lock before checking availability so another request cannot select the same car at the same time.
        reservationLock.lockCarType(type);

        Car car = carAvailability.findAvailableCar(type, period)
                .orElseThrow(NoAvailableCarException::new);

        return reservationStore.save(car, period);
    }
}
