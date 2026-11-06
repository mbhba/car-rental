package com.example.carrental.domain;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;

public final class Period {

    private static final long SECONDS_PER_DAY = 86_400;

    private final OffsetDateTime start;
    private final OffsetDateTime end;

    private Period(OffsetDateTime start, OffsetDateTime end) {
        this.start = start;
        this.end = end;
    }

    public static Period of(OffsetDateTime start, OffsetDateTime end) {
        Objects.requireNonNull(start, "Period start is required");
        Objects.requireNonNull(end, "Period end is required");
        if (!start.toInstant().isBefore(end.toInstant())) {
            throw new IllegalArgumentException("Period end must be after its start");
        }
        return new Period(start, end);
    }

    public static Period forDays(OffsetDateTime start, int days) {
        Objects.requireNonNull(start, "Period start is required");
        if (days <= 0) {
            throw new IllegalArgumentException("Period length must be positive");
        }

        // Use elapsed seconds so each requested day always means exactly 24 hours.
        long seconds = Math.multiplyExact((long) days, SECONDS_PER_DAY);
        Instant endInstant = start.toInstant().plusSeconds(seconds);
        OffsetDateTime end = OffsetDateTime.ofInstant(endInstant, start.getOffset());
        return new Period(start, end);
    }

    public OffsetDateTime start() {
        return start;
    }

    public OffsetDateTime end() {
        return end;
    }

    public boolean contains(OffsetDateTime dateTime) {
        Objects.requireNonNull(dateTime, "Date and time is required");
        Instant instant = dateTime.toInstant();
        return !instant.isBefore(start.toInstant()) && instant.isBefore(end.toInstant());
    }

    public boolean overlaps(Period other) {
        Objects.requireNonNull(other, "Other period is required");
        return start.toInstant().isBefore(other.end.toInstant())
                && other.start.toInstant().isBefore(end.toInstant());
    }
}
