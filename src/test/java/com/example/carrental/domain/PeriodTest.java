package com.example.carrental.domain;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodTest {

    @Test
    void forDaysUsesExactlyTwentyFourHoursPerDay() {
        OffsetDateTime start = OffsetDateTime.parse("2026-03-28T12:30:00+01:00");

        Period period = Period.forDays(start, 2);

        assertThat(Duration.between(period.start().toInstant(), period.end().toInstant()))
                .isEqualTo(Duration.ofHours(48));
        assertThat(period.end().getOffset()).isEqualTo(start.getOffset());
    }

    @Test
    void forDaysRejectsZeroAndNegativeLengths() {
        OffsetDateTime start = OffsetDateTime.parse("2026-03-28T12:30:00+01:00");

        assertThatThrownBy(() -> Period.forDays(start, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Period.forDays(start, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofRequiresAnEndAfterTheStart() {
        OffsetDateTime start = OffsetDateTime.parse("2026-03-28T12:30:00+01:00");

        assertThatThrownBy(() -> Period.of(start, start))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Period.of(start, start.minusHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void includesItsStartAndExcludesItsEnd() {
        OffsetDateTime start = OffsetDateTime.parse("2026-03-28T12:30:00+01:00");
        Period period = Period.forDays(start, 1);

        assertThat(period.contains(start)).isTrue();
        assertThat(period.contains(period.end())).isFalse();
    }

    @Test
    void adjacentPeriodsDoNotOverlap() {
        OffsetDateTime start = OffsetDateTime.parse("2026-03-28T12:30:00+01:00");
        Period first = Period.forDays(start, 1);
        Period second = Period.of(first.end(), first.end().plusDays(1));

        assertThat(first.overlaps(second)).isFalse();
        assertThat(second.overlaps(first)).isFalse();
    }

    @Test
    void comparesPeriodBoundariesByInstantAcrossOffsets() {
        Period period = Period.of(
                OffsetDateTime.parse("2026-03-28T12:00:00+01:00"),
                OffsetDateTime.parse("2026-03-28T14:00:00+01:00"));

        assertThat(period.contains(OffsetDateTime.parse("2026-03-28T12:30:00Z"))).isTrue();
        assertThat(period.contains(OffsetDateTime.parse("2026-03-28T13:00:00Z"))).isFalse();
    }
}
