package com.example.carrental.application;

public final class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(Throwable cause) {
        super("The requested period is no longer available", cause);
    }
}
