package com.example.carrental.adapter.http;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.example.carrental.application.InvalidReservationException;
import com.example.carrental.application.ReserveByType;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationControllerTest {

    @Test
    void createsAReservationAndReturnsItsDetails() {
        ReserveByType reserveByType = mock(ReserveByType.class);
        Reservation reservation = new Reservation(
                12,
                new Car(3, CarType.VAN),
                Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 2));
        when(reserveByType.reserve(CarType.VAN, reservation.period().start(), 2)).thenReturn(reservation);
        ReservationController controller = new ReservationController(reserveByType);
        CreateReservationRequest request = new CreateReservationRequest(
                CarType.VAN, reservation.period().start(), 2);

        var response = controller.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(ReservationResponse.from(reservation));
        verify(reserveByType).reserve(CarType.VAN, reservation.period().start(), 2);
    }

    @Test
    void rejectsAMissingRequest() {
        ReservationController controller = new ReservationController(mock(ReserveByType.class));

        assertThatThrownBy(() -> controller.create(null))
                .isInstanceOf(InvalidReservationException.class);
    }
}
