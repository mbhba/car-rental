package com.example.carrental.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.carrental.adapter.persistence.TransactionalCancelReservation;
import com.example.carrental.adapter.persistence.TransactionalReserveCar;
import com.example.carrental.application.CancelReservation;
import com.example.carrental.application.CancelReservationService;
import com.example.carrental.application.FindReservation;
import com.example.carrental.application.FindReservationService;
import com.example.carrental.application.ReserveCar;
import com.example.carrental.application.ReserveCarService;
import com.example.carrental.application.SearchAvailableCars;
import com.example.carrental.application.SearchAvailableCarsService;
import com.example.carrental.application.port.CarAvailabilityPort;
import com.example.carrental.application.port.ReservationLockPort;
import com.example.carrental.application.port.ReservationStorePort;

@Configuration
public class ApplicationConfiguration {

    @Bean
    ReserveCar reserveCar(
            CarAvailabilityPort carAvailability,
            ReservationLockPort reservationLock,
            ReservationStorePort reservationStore,
            Clock clock) {
        ReserveCar useCase = new ReserveCarService(
                carAvailability, reservationLock, reservationStore, clock);
        return new TransactionalReserveCar(useCase);
    }

    @Bean
    SearchAvailableCars searchAvailableCars(CarAvailabilityPort carAvailability, Clock clock) {
        return new SearchAvailableCarsService(carAvailability, clock);
    }

    @Bean
    FindReservation findReservation(ReservationStorePort reservationStore) {
        return new FindReservationService(reservationStore);
    }

    @Bean
    CancelReservation cancelReservation(
            ReservationStorePort reservationStore,
            ReservationLockPort reservationLock) {
        return new TransactionalCancelReservation(
                new CancelReservationService(reservationStore, reservationLock));
    }
}
