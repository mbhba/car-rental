package com.example.carrental.application;

public final class NoAvailableCarException extends RuntimeException {

    public NoAvailableCarException() {
        super("No car is available for the requested period");
    }
}
