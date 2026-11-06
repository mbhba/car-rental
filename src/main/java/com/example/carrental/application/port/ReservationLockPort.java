package com.example.carrental.application.port;

import com.example.carrental.domain.CarType;

public interface ReservationLockPort {

    void lockCarType(CarType type);
}
