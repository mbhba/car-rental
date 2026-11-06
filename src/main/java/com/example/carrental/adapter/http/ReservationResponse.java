package com.example.carrental.adapter.http;

import java.time.OffsetDateTime;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.domain.ReservationStatus;

public record ReservationResponse(
        long id,
        long carId,
        CarType carType,
        OffsetDateTime start,
        OffsetDateTime end,
        ReservationStatus status) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.id(),
                reservation.car().id(),
                reservation.car().type(),
                reservation.period().start(),
                reservation.period().end(),
                reservation.status());
    }
}
