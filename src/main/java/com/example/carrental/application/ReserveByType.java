package com.example.carrental.application;

import java.time.OffsetDateTime;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;

public interface ReserveByType {

    Reservation reserve(CarType type, OffsetDateTime start, Integer days);
}
