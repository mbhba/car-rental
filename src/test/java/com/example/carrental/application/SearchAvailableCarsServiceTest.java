package com.example.carrental.application;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.carrental.application.port.CarAvailabilityPort;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchAvailableCarsServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-06T10:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-08T10:00:00Z");

    @Test
    void searchesCarsWithoutATypeFilter() {
        CarAvailabilityPort availability = mock(CarAvailabilityPort.class);
        List<Car> cars = List.of(new Car(1, CarType.SEDAN));
        when(availability.findAvailableCars(any(Period.class))).thenReturn(cars);
        SearchAvailableCarsService service = new SearchAvailableCarsService(availability, CLOCK);

        assertThat(service.search(START, 2)).isSameAs(cars);
        verify(availability).findAvailableCars(any(Period.class));
    }

    @Test
    void searchesCarsWithTypeAndPeriod() {
        CarAvailabilityPort availability = mock(CarAvailabilityPort.class);
        List<Car> cars = List.of(new Car(1, CarType.SEDAN));
        when(availability.findAvailableCars(eq(CarType.SEDAN), any(Period.class)))
                .thenReturn(cars);
        SearchAvailableCarsService service = new SearchAvailableCarsService(availability, CLOCK);

        assertThat(service.search(CarType.SEDAN, START, 2)).isSameAs(cars);
        verify(availability).findAvailableCars(eq(CarType.SEDAN), any(Period.class));
    }

    @Test
    void rejectsInvalidOrPastSearchPeriodsBeforeSearching() {
        CarAvailabilityPort availability = mock(CarAvailabilityPort.class);
        SearchAvailableCarsService service = new SearchAvailableCarsService(availability, CLOCK);

        assertThatThrownBy(() -> service.search((CarType) null, START, 1))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.search(null, 1))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.search(START, 0))
                .isInstanceOf(InvalidReservationException.class);
        assertThatThrownBy(() -> service.search(
                OffsetDateTime.parse("2026-11-06T09:00:00Z"), 1))
                .isInstanceOf(InvalidReservationException.class);
    }
}
