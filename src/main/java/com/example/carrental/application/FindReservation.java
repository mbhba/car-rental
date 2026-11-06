package com.example.carrental.application;

import com.example.carrental.domain.Reservation;

public interface FindReservation {

    Reservation find(long reservationId);
}
