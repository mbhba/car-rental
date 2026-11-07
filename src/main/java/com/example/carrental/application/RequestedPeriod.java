package com.example.carrental.application;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.OffsetDateTime;

import com.example.carrental.domain.Period;

/** Applies the shared request rules before search or reservation work starts. */
public final class RequestedPeriod {

    static final int MAX_RESERVATION_DAYS = 365;
    private static final Duration MAX_START_ADVANCE = Duration.ofDays(MAX_RESERVATION_DAYS);

    private final Clock clock;

    public RequestedPeriod(Clock clock) {
        this.clock = clock;
    }

    public Period create(OffsetDateTime start, Integer days) {
        if (start == null || days == null) {
            throw new InvalidReservationException("Start and days are required");
        }

        if (days < 1 || days > MAX_RESERVATION_DAYS) {
            throw new InvalidReservationException(
                    "The reservation length must be between 1 and " + MAX_RESERVATION_DAYS + " days");
        }

        var now = clock.instant();
        if (start.toInstant().isBefore(now)) {
            throw new InvalidReservationException("The reservation start must not be in the past");
        }
        if (start.toInstant().isAfter(now.plus(MAX_START_ADVANCE))) {
            throw new InvalidReservationException(
                    "The reservation start must be within " + MAX_RESERVATION_DAYS + " days");
        }

        Period period;
        try {
            period = Period.forDays(start, days);
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new InvalidReservationException("The reservation period is invalid", exception);
        }

        return period;
    }
}
