package com.example.carrental.application.port;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.Period;
import com.example.carrental.domain.Reservation;

public interface ReservationStorePort {

    Reservation save(Car car, Period period);
}
