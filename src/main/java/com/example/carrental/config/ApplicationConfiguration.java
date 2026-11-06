package com.example.carrental.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.carrental.application.ReserveByType;
import com.example.carrental.application.ReserveByTypeService;
import com.example.carrental.application.port.CarAvailabilityPort;
import com.example.carrental.application.port.ReservationStorePort;

@Configuration
public class ApplicationConfiguration {

    @Bean
    ReserveByType reserveByType(
            CarAvailabilityPort carAvailability,
            ReservationStorePort reservationStore,
            Clock clock) {
        return new ReserveByTypeService(carAvailability, reservationStore, clock);
    }
}
