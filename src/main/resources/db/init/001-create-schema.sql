CREATE TABLE car_type (
    code VARCHAR(16) PRIMARY KEY,
    CONSTRAINT car_type_code_valid
        CHECK (code IN ('SEDAN', 'SUV', 'VAN'))
);

INSERT INTO car_type (code) VALUES
    ('SEDAN'),
    ('SUV'),
    ('VAN');

-- Keep each car type as a row so Phase 5 can lock one type across app instances.
CREATE TABLE car (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type_code VARCHAR(16) NOT NULL,
    CONSTRAINT car_id_positive CHECK (id > 0),
    CONSTRAINT car_type_exists
        FOREIGN KEY (type_code) REFERENCES car_type (code)
);

INSERT INTO car (type_code) VALUES
    ('SEDAN'),
    ('SEDAN'),
    ('SUV'),
    ('SUV'),
    ('VAN'),
    ('VAN');

-- btree_gist lets the exclusion constraint compare car IDs and time ranges in one GiST index.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE reservation (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    car_id BIGINT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT reservation_id_positive CHECK (id > 0),
    CONSTRAINT reservation_car_exists
        FOREIGN KEY (car_id) REFERENCES car (id),
    CONSTRAINT reservation_period_valid CHECK (ends_at > starts_at),
    CONSTRAINT reservation_status_valid
        CHECK (status IN ('ACTIVE', 'CANCELLED')),
    -- Protect writes that bypass the type lock and ignore cancelled periods.
    CONSTRAINT reservation_no_overlapping_periods
        EXCLUDE USING GIST (
            car_id WITH =,
            tstzrange(starts_at, ends_at, '[)') WITH &&
        ) WHERE (status = 'ACTIVE')
);
