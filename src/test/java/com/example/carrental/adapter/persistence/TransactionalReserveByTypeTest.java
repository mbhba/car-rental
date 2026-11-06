package com.example.carrental.adapter.persistence;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.example.carrental.application.ReserveByType;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionalReserveByTypeTest {

    @Test
    void delegatesTheReservationRequest() {
        ReserveByType delegate = mock(ReserveByType.class);
        OffsetDateTime start = OffsetDateTime.parse("2026-11-08T10:00:00Z");
        Reservation reservation = new Reservation(4, new Car(2, CarType.SEDAN), Period.forDays(start, 1));
        when(delegate.reserve(CarType.SEDAN, start, 1)).thenReturn(reservation);
        TransactionalReserveByType transactional = new TransactionalReserveByType(delegate);

        assertThat(transactional.reserve(CarType.SEDAN, start, 1)).isSameAs(reservation);
        verify(delegate).reserve(CarType.SEDAN, start, 1);
    }
}
