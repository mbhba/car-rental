package com.example.carrental.adapter.http;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.carrental.application.CancelReservation;
import com.example.carrental.application.FindReservation;
import com.example.carrental.application.InvalidReservationException;
import com.example.carrental.application.ReserveCar;
import com.example.carrental.application.SearchAvailableCars;
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
    void createsAReservationByTypeAndReturnsItsDetails() {
        ReserveCar reserveCar = mock(ReserveCar.class);
        FindReservation findReservation = mock(FindReservation.class);
        CancelReservation cancelReservation = mock(CancelReservation.class);
        Reservation reservation = reservation(12, 3, CarType.VAN);
        when(reserveCar.reserve(CarType.VAN, null, reservation.period().start(), 2)).thenReturn(reservation);
        ReservationController controller = new ReservationController(reserveCar, findReservation, cancelReservation);
        CreateReservationRequest request = new CreateReservationRequest(
                CarType.VAN, null, reservation.period().start(), 2);

        var response = controller.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(ReservationResponse.from(reservation));
        verify(reserveCar).reserve(CarType.VAN, null, reservation.period().start(), 2);
    }

    @Test
    void createsAReservationByCarId() {
        ReserveCar reserveCar = mock(ReserveCar.class);
        Reservation reservation = reservation(12, 3, CarType.VAN);
        when(reserveCar.reserve(null, 3L, reservation.period().start(), 2)).thenReturn(reservation);
        ReservationController controller = new ReservationController(
                reserveCar, mock(FindReservation.class), mock(CancelReservation.class));

        var response = controller.create(new CreateReservationRequest(
                null, 3L, reservation.period().start(), 2));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(reserveCar).reserve(null, 3L, reservation.period().start(), 2);
    }

    @Test
    void findsAndCancelsReservations() {
        FindReservation findReservation = mock(FindReservation.class);
        CancelReservation cancelReservation = mock(CancelReservation.class);
        Reservation reservation = reservation(12, 3, CarType.VAN);
        when(findReservation.find(12)).thenReturn(reservation);
        when(cancelReservation.cancel(12)).thenReturn(reservation);
        ReservationController controller = new ReservationController(
                mock(ReserveCar.class), findReservation, cancelReservation);

        assertThat(controller.find(12)).isEqualTo(ReservationResponse.from(reservation));
        assertThat(controller.cancel(12)).isEqualTo(ReservationResponse.from(reservation));
        verify(findReservation).find(12);
        verify(cancelReservation).cancel(12);
    }

    @Test
    void rejectsAMissingRequest() {
        ReservationController controller = new ReservationController(
                mock(ReserveCar.class), mock(FindReservation.class), mock(CancelReservation.class));

        assertThatThrownBy(() -> controller.create(null))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void mapsAvailableCarsToApiResponses() {
        SearchAvailableCars searchAvailableCars = mock(SearchAvailableCars.class);
        OffsetDateTime start = OffsetDateTime.parse("2026-11-08T10:00:00Z");
        when(searchAvailableCars.search(start, 1)).thenReturn(List.of(
                new Car(1, CarType.SEDAN), new Car(4, CarType.VAN)));
        var controller = new CarAvailabilityController(searchAvailableCars);

        var response = controller.available(start, 1, null);

        assertThat(response).containsExactly(
                new CarAvailabilityResponse(1, CarType.SEDAN),
                new CarAvailabilityResponse(4, CarType.VAN));
        verify(searchAvailableCars).search(start, 1);
    }

    private Reservation reservation(long id, long carId, CarType type) {
        return new Reservation(id, new Car(carId, type), Period.forDays(
                OffsetDateTime.parse("2026-11-08T10:00:00Z"), 2));
    }
}
