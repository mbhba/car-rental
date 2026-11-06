package com.example.carrental.application;

import com.example.carrental.domain.Reservation;

public interface CancelReservation {

    Reservation cancel(long reservationId);
}
