-- Keep this migration repeat-safe for new and existing database volumes.
CREATE EXTENSION IF NOT EXISTS btree_gist;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'reservation'::regclass
          AND conname = 'reservation_no_overlapping_periods'
    ) THEN
        ALTER TABLE reservation
            ADD CONSTRAINT reservation_no_overlapping_periods
            EXCLUDE USING GIST (
                car_id WITH =,
                tstzrange(starts_at, ends_at, '[)') WITH &&
            );
    END IF;
END
$$;
