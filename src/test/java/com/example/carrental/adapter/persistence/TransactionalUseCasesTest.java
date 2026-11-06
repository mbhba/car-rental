package com.example.carrental.adapter.persistence;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.example.carrental.application.CancelReservation;
import com.example.carrental.application.ReserveCar;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionalUseCasesTest {

    @Test
    void delegatesReservationRequests() {
        ReserveCar delegate = mock(ReserveCar.class);
        OffsetDateTime start = OffsetDateTime.parse("2026-11-08T10:00:00Z");
        Reservation reservation = new Reservation(4, new Car(2, CarType.SEDAN), Period.forDays(start, 1));
        when(delegate.reserve(CarType.SEDAN, null, start, 1)).thenReturn(reservation);
        TransactionalReserveCar transactional = new TransactionalReserveCar(delegate);

        assertThat(transactional.reserve(CarType.SEDAN, null, start, 1)).isSameAs(reservation);
        verify(delegate).reserve(CarType.SEDAN, null, start, 1);
    }

    @Test
    void delegatesCancellationRequests() {
        CancelReservation delegate = mock(CancelReservation.class);
        Reservation reservation = new Reservation(4, new Car(2, CarType.SEDAN),
                Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 1));
        when(delegate.cancel(4)).thenReturn(reservation);
        TransactionalCancelReservation transactional = new TransactionalCancelReservation(delegate);

        assertThat(transactional.cancel(4)).isSameAs(reservation);
        verify(delegate).cancel(4);
    }
}
