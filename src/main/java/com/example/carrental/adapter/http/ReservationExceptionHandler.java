package com.example.carrental.adapter.http;

import java.time.DateTimeException;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.carrental.application.InvalidReservationException;
import com.example.carrental.application.NoAvailableCarException;
import com.example.carrental.application.ReservationConflictException;

@RestControllerAdvice
public class ReservationExceptionHandler {

    @ExceptionHandler({InvalidReservationException.class, HttpMessageNotReadableException.class, DateTimeException.class})
    public ResponseEntity<ApiError> badRequest(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiError("Invalid reservation request"));
    }

    @ExceptionHandler(NoAvailableCarException.class)
    public ResponseEntity<ApiError> noAvailability(NoAvailableCarException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ApiError> reservationConflict(ReservationConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> databaseError(DataAccessException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("The reservation could not be saved"));
    }

    public record ApiError(String message) {
    }
}
