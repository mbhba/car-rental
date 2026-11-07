-- Cancellation frees a car period, while the exclusion constraint still protects active reservations.
-- Keep the old constraint if creating the new constraint fails.
BEGIN;

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservation DROP CONSTRAINT IF EXISTS reservation_no_overlapping_periods;

ALTER TABLE reservation
    ADD CONSTRAINT reservation_no_overlapping_periods
    EXCLUDE USING GIST (
        car_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    ) WHERE (status = 'ACTIVE');

COMMIT;
