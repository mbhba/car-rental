package com.example.carrental.adapter.http;

import java.time.DateTimeException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import com.example.carrental.application.InvalidReservationException;
import com.example.carrental.application.NoAvailableCarException;
import com.example.carrental.application.ReservationConflictException;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationExceptionHandlerTest {

    private final ReservationExceptionHandler handler = new ReservationExceptionHandler();

    @Test
    void mapsInvalidRequestsToBadRequest() {
        var response = handler.badRequest(new InvalidReservationException("invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Invalid reservation request");
    }

    @Test
    void mapsDateErrorsToBadRequest() {
        assertThat(handler.badRequest(new DateTimeException("invalid date")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsNoAvailabilityAndDatabaseConflictsToConflict() {
        assertThat(handler.noAvailability(new NoAvailableCarException()).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.reservationConflict(new ReservationConflictException(new RuntimeException()))
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void hidesDetailsFromUnexpectedDatabaseErrors() {
        var response = handler.databaseError(new DataAccessResourceFailureException("private database detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("The reservation could not be saved");
    }
}
