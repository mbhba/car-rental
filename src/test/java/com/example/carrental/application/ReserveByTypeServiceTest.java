package com.example.carrental.application;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.example.carrental.application.port.CarAvailabilityPort;
import com.example.carrental.application.port.ReservationStorePort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReserveByTypeServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void reservesAnAvailableCarForTheRequestedTypeAndPeriod() {
        Car car = new Car(2, CarType.SUV);
        AtomicReference<Period> searchedPeriod = new AtomicReference<>();
        AtomicReference<CarType> searchedType = new AtomicReference<>();
        AtomicReference<Period> savedPeriod = new AtomicReference<>();
        CarAvailabilityPort availability = (type, period) -> {
            searchedType.set(type);
            searchedPeriod.set(period);
            return Optional.of(car);
        };
        ReservationStorePort store = (savedCar, period) -> {
            savedPeriod.set(period);
            return new Reservation(17, savedCar, period);
        };
        ReserveByTypeService service = new ReserveByTypeService(availability, type -> {}, store, CLOCK);
        OffsetDateTime start = OffsetDateTime.parse("2026-11-08T12:00:00+02:00");

        Reservation reservation = service.reserve(CarType.SUV, start, 3);

        assertThat(reservation.id()).isEqualTo(17);
        assertThat(reservation.car()).isSameAs(car);
        assertThat(searchedType.get()).isEqualTo(CarType.SUV);
        assertThat(searchedPeriod.get().start()).isEqualTo(start);
        assertThat(searchedPeriod.get().end()).isEqualTo(start.plusHours(72));
        assertThat(savedPeriod.get()).isSameAs(searchedPeriod.get());
    }

    @Test
    void rejectsPastStartsBeforeSearchingForACar() {
        AtomicReference<Boolean> searched = new AtomicReference<>(false);
        CarAvailabilityPort availability = (type, period) -> {
            searched.set(true);
            return Optional.of(new Car(1, type));
        };
        ReservationStorePort store = (car, period) -> new Reservation(1, car, period);
        ReserveByTypeService service = new ReserveByTypeService(availability, type -> {}, store, CLOCK);

        assertThatThrownBy(() -> service.reserve(
                CarType.SEDAN, OffsetDateTime.parse("2026-11-06T09:59:59Z"), 1))
                .isInstanceOf(InvalidReservationException.class);
        assertThat(searched.get()).isFalse();
    }

    @Test
    void rejectsMissingAndInvalidReservationValues() {
        ReserveByTypeService service = new ReserveByTypeService(
                (type, period) -> Optional.empty(),
                type -> {},
                (car, period) -> new Reservation(1, car, period),
                CLOCK);
        OffsetDateTime future = OffsetDateTime.parse("2026-11-08T10:00:00Z");

        assertThatThrownBy(() -> service.reserve(null, future, 1))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, null, 1))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, future, null))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, future, 0))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void reportsWhenNoCarIsAvailable() {
        ReserveByTypeService service = new ReserveByTypeService(
                (type, period) -> Optional.empty(),
                type -> {},
                (car, period) -> new Reservation(1, car, period),
                CLOCK);

        assertThatThrownBy(() -> service.reserve(
                CarType.VAN, OffsetDateTime.parse("2026-11-08T10:00:00Z"), 1))
                .isInstanceOf(NoAvailableCarException.class);
    }
}
