package com.example.carrental.application;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.OffsetDateTime;

import com.example.carrental.domain.Period;

/** Applies the shared request rules before search or reservation work starts. */
public final class RequestedPeriod {

    private final Clock clock;

    public RequestedPeriod(Clock clock) {
        this.clock = clock;
    }

    public Period create(OffsetDateTime start, Integer days) {
        if (start == null || days == null) {
            throw new InvalidReservationException("Start and days are required");
        }

        Period period;
        try {
            period = Period.forDays(start, days);
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new InvalidReservationException("The reservation period is invalid", exception);
        }

        if (period.start().toInstant().isBefore(clock.instant())) {
            throw new InvalidReservationException("The reservation start must not be in the past");
        }
        return period;
    }
}
