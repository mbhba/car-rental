package com.example.carrental.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestedPeriodTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");
    private static final RequestedPeriod REQUESTED_PERIOD = new RequestedPeriod(
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void accepts365DaysAndAStartExactly365DaysAhead() {
        OffsetDateTime start = OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(365)), ZoneOffset.UTC);

        var period = REQUESTED_PERIOD.create(start, 365);

        assertThat(period.start()).isEqualTo(start);
        assertThat(period.end()).isEqualTo(start.plusDays(365));
    }

    @Test
    void rejectsPeriodsLongerThan365Days() {
        assertThatThrownBy(() -> REQUESTED_PERIOD.create(
                OffsetDateTime.ofInstant(NOW.plusSeconds(1), ZoneOffset.UTC), 366))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void rejectsStartsMoreThan365DaysAhead() {
        OffsetDateTime start = OffsetDateTime.ofInstant(
                NOW.plus(Duration.ofDays(365)).plusSeconds(1), ZoneOffset.UTC);

        assertThatThrownBy(() -> REQUESTED_PERIOD.create(start, 1))
                .isInstanceOf(InvalidReservationException.class);
    }
}
