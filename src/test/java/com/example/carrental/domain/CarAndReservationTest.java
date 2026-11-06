package com.example.carrental.domain;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarAndReservationTest {

    @Test
    void carRequiresAPositiveIdAndType() {
        assertThatThrownBy(() -> new Car(0, CarType.SEDAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Car(1, null))
                .isInstanceOf(NullPointerException.class);

        Car car = new Car(1, CarType.VAN);

        assertThat(car.id()).isEqualTo(1);
        assertThat(car.type()).isEqualTo(CarType.VAN);
    }

    @Test
    void reservationStartsActiveAndCanBeCancelledMoreThanOnce() {
        Car car = new Car(1, CarType.SEDAN);
        Period period = Period.forDays(OffsetDateTime.parse("2026-03-28T12:30:00Z"), 1);
        Reservation reservation = new Reservation(42, car, period);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);

        reservation.cancel();
        reservation.cancel();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.car()).isSameAs(car);
        assertThat(reservation.period()).isSameAs(period);
    }

    @Test
    void reservationRequiresAnIdCarAndPeriod() {
        Car car = new Car(1, CarType.SEDAN);
        Period period = Period.forDays(OffsetDateTime.parse("2026-03-28T12:30:00Z"), 1);

        assertThatThrownBy(() -> new Reservation(0, car, period))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Reservation(1, null, period))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Reservation(1, car, null))
                .isInstanceOf(NullPointerException.class);
    }
}
