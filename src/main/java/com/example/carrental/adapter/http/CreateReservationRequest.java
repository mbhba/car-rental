package com.example.carrental.adapter.http;

import java.time.OffsetDateTime;

import com.example.carrental.domain.CarType;

public record CreateReservationRequest(CarType carType, Long carId, OffsetDateTime start, Integer days) {
}
