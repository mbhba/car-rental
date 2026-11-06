package com.example.carrental.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import com.example.carrental.application.port.CarAvailabilityPort;
import com.example.carrental.application.port.ReservationLockPort;
import com.example.carrental.application.port.ReservationStorePort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;

public final class ReserveCarService implements ReserveCar {

    private final CarAvailabilityPort carAvailability;
    private final ReservationLockPort reservationLock;
    private final ReservationStorePort reservationStore;
    private final RequestedPeriod requestedPeriod;

    public ReserveCarService(
            CarAvailabilityPort carAvailability,
            ReservationLockPort reservationLock,
            ReservationStorePort reservationStore,
            Clock clock) {
        this.carAvailability = carAvailability;
        this.reservationLock = reservationLock;
        this.reservationStore = reservationStore;
        this.requestedPeriod = new RequestedPeriod(clock);
    }

    @Override
    public Reservation reserve(CarType type, Long carId, OffsetDateTime start, Integer days) {
        if ((type == null) == (carId == null)) {
            throw new InvalidReservationException("Provide exactly one of carType or carId");
        }
        if (carId != null && carId <= 0) {
            throw new InvalidReservationException("Car ID must be positive");
        }

        Period period = requestedPeriod.create(start, days);
        Car requestedCar = carId == null
                ? null
                : carAvailability.findCarById(carId).orElseThrow(() -> new CarNotFoundException(carId));
        CarType lockedType = type == null ? requestedCar.type() : type;

        // Resolve an ID to its type first, then take the same cross-instance lock used for type searches.
        reservationLock.lockCarType(lockedType);

        Car car = requestedCar == null
                ? carAvailability.findAvailableCars(type, period).stream()
                        .findFirst().orElseThrow(NoAvailableCarException::new)
                : carAvailability.findAvailableCar(carId, period)
                        .orElseThrow(NoAvailableCarException::new);

        return reservationStore.save(car, period);
    }
}
