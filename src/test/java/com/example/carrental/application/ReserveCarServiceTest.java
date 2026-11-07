package com.example.carrental.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.carrental.application.port.CarAvailabilityPort;
import com.example.carrental.application.port.ReservationLockPort;
import com.example.carrental.application.port.ReservationStorePort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReserveCarServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final OffsetDateTime FUTURE = OffsetDateTime.parse("2026-11-08T12:00:00+02:00");

    @Test
    void reservesAnAvailableCarOfTheRequestedType() {
        CarAvailabilityPort availability = mock(CarAvailabilityPort.class);
        ReservationLockPort lock = mock(ReservationLockPort.class);
        ReservationStorePort store = mock(ReservationStorePort.class);
        Car car = new Car(2, CarType.SUV);
        when(availability.findAvailableCars(eq(CarType.SUV), any(Period.class)))
                .thenReturn(List.of(car));
        when(store.save(eq(car), any(Period.class)))
                .thenReturn(new Reservation(17, car, Period.forDays(FUTURE, 3)));
        ReserveCarService service = new ReserveCarService(availability, lock, store, CLOCK);

        Reservation reservation = service.reserve(CarType.SUV, null, FUTURE, 3);

        assertThat(reservation.id()).isEqualTo(17);
        assertThat(reservation.car()).isSameAs(car);
        var order = inOrder(lock, availability, store);
        order.verify(lock).lockCarType(CarType.SUV);
        order.verify(availability).findAvailableCars(eq(CarType.SUV), any(Period.class));
        order.verify(store).save(eq(car), any(Period.class));
    }

    @Test
    void resolvesCarTypeBeforeLockAndChecksTheRequestedCarAgain() {
        CarAvailabilityPort availability = mock(CarAvailabilityPort.class);
        ReservationLockPort lock = mock(ReservationLockPort.class);
        ReservationStorePort store = mock(ReservationStorePort.class);
        Car car = new Car(5, CarType.VAN);
        when(availability.findCarById(5)).thenReturn(Optional.of(car));
        when(availability.findAvailableCar(eq(5L), any(Period.class))).thenReturn(Optional.of(car));
        when(store.save(eq(car), any(Period.class)))
                .thenReturn(new Reservation(18, car, Period.forDays(FUTURE, 1)));
        ReserveCarService service = new ReserveCarService(availability, lock, store, CLOCK);

        Reservation reservation = service.reserve(null, 5L, FUTURE, 1);

        assertThat(reservation.car()).isSameAs(car);
        var order = inOrder(availability, lock, store);
        order.verify(availability).findCarById(5);
        order.verify(lock).lockCarType(CarType.VAN);
        order.verify(availability).findAvailableCar(eq(5L), any(Period.class));
        order.verify(store).save(eq(car), any(Period.class));
    }

    @Test
    void reportsUnknownCarWithoutTakingTheTypeLock() {
        CarAvailabilityPort availability = mock(CarAvailabilityPort.class);
        ReservationLockPort lock = mock(ReservationLockPort.class);
        ReservationStorePort store = mock(ReservationStorePort.class);
        when(availability.findCarById(999)).thenReturn(Optional.empty());
        ReserveCarService service = new ReserveCarService(availability, lock, store, CLOCK);

        assertThatThrownBy(() -> service.reserve(null, 999L, FUTURE, 1))
                .isInstanceOf(CarNotFoundException.class);
        verify(lock, never()).lockCarType(CarType.SEDAN);
        verify(store, never()).save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportsBusyRequestedCar() {
        CarAvailabilityPort availability = mock(CarAvailabilityPort.class);
        ReservationLockPort lock = mock(ReservationLockPort.class);
        ReservationStorePort store = mock(ReservationStorePort.class);
        when(availability.findCarById(5)).thenReturn(Optional.of(new Car(5, CarType.VAN)));
        when(availability.findAvailableCar(eq(5L), any(Period.class))).thenReturn(Optional.empty());
        ReserveCarService service = new ReserveCarService(availability, lock, store, CLOCK);

        assertThatThrownBy(() -> service.reserve(null, 5L, FUTURE, 1))
                .isInstanceOf(NoAvailableCarException.class);
        verify(store, never()).save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInvalidReservationInputsBeforeDatabaseWork() {
        CarAvailabilityPort availability = mock(CarAvailabilityPort.class);
        ReservationLockPort lock = mock(ReservationLockPort.class);
        ReservationStorePort store = mock(ReservationStorePort.class);
        ReserveCarService service = new ReserveCarService(availability, lock, store, CLOCK);

        assertThatThrownBy(() -> service.reserve(null, null, FUTURE, 1))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, 1L, FUTURE, 1))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, null, FUTURE, 0))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, null, FUTURE, 366))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, null,
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(365)).plusSeconds(1), ZoneOffset.UTC), 1))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, null,
                OffsetDateTime.parse("2026-11-06T09:59:59Z"), 1))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(null, 0L, FUTURE, 1))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, null, OffsetDateTime.MAX, 1))
                .isInstanceOf(InvalidReservationException.class);
        verifyNoInteractions(availability, lock, store);
    }

    @Test
    void reportsWhenNoCarOfTheRequestedTypeIsAvailable() {
        CarAvailabilityPort availability = mock(CarAvailabilityPort.class);
        when(availability.findAvailableCars(eq(CarType.VAN), any(Period.class)))
                .thenReturn(List.of());
        ReserveCarService service = new ReserveCarService(
                availability, mock(ReservationLockPort.class), mock(ReservationStorePort.class), CLOCK);

        assertThatThrownBy(() -> service.reserve(CarType.VAN, null, FUTURE, 1))
                .isInstanceOf(NoAvailableCarException.class);
    }
}
