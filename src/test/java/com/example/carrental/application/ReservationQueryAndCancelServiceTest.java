package com.example.carrental.application;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.carrental.application.port.ReservationLockPort;
import com.example.carrental.application.port.ReservationStorePort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;
import com.example.carrental.domain.ReservationStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationQueryAndCancelServiceTest {

    @Test
    void findsExistingReservationsAndReportsUnknownIds() {
        ReservationStorePort store = mock(ReservationStorePort.class);
        Reservation reservation = reservation(9, ReservationStatus.ACTIVE);
        when(store.findById(9)).thenReturn(Optional.of(reservation));
        FindReservationService service = new FindReservationService(store);

        assertThat(service.find(9)).isSameAs(reservation);
        when(store.findById(10)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.find(10)).isInstanceOf(ReservationNotFoundException.class);
        assertThatThrownBy(() -> service.find(0)).isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void cancelsUnderTypeThenReservationLockAndReturnsCurrentStatus() {
        ReservationStorePort store = mock(ReservationStorePort.class);
        ReservationLockPort lock = mock(ReservationLockPort.class);
        Reservation existing = reservation(9, ReservationStatus.ACTIVE);
        Reservation locked = reservation(9, ReservationStatus.ACTIVE);
        when(store.findById(9)).thenReturn(Optional.of(existing));
        when(store.findByIdForUpdate(9)).thenReturn(Optional.of(locked));
        CancelReservationService service = new CancelReservationService(store, lock);

        Reservation cancelled = service.cancel(9);

        assertThat(cancelled.status()).isEqualTo(ReservationStatus.CANCELLED);
        var order = inOrder(store, lock);
        order.verify(store).findById(9);
        order.verify(lock).lockCarType(CarType.VAN);
        order.verify(store).findByIdForUpdate(9);
        order.verify(store).update(locked);
    }

    @Test
    void repeatsCancellationSafelyAndDoesNotSaveUnknownIds() {
        ReservationStorePort store = mock(ReservationStorePort.class);
        ReservationLockPort lock = mock(ReservationLockPort.class);
        Reservation cancelled = reservation(9, ReservationStatus.CANCELLED);
        when(store.findById(9)).thenReturn(Optional.of(cancelled));
        when(store.findByIdForUpdate(9)).thenReturn(Optional.of(cancelled));
        when(store.findById(10)).thenReturn(Optional.empty());
        CancelReservationService service = new CancelReservationService(store, lock);

        assertThat(service.cancel(9).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThatThrownBy(() -> service.cancel(10)).isInstanceOf(ReservationNotFoundException.class);
        assertThatThrownBy(() -> service.cancel(-1)).isInstanceOf(InvalidReservationException.class);
        verify(store).update(cancelled);
        verify(lock, never()).lockCarType(CarType.SEDAN);
    }

    private Reservation reservation(long id, ReservationStatus status) {
        return new Reservation(id, new Car(2, CarType.VAN),
                Period.forDays(OffsetDateTime.parse("2026-11-08T10:00:00Z"), 1), status);
    }
}
