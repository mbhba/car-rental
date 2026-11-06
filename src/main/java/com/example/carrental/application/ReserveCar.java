package com.example.carrental.application;

import java.time.OffsetDateTime;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;

public interface ReserveCar {

    Reservation reserve(CarType type, Long carId, OffsetDateTime start, Integer days);
}
