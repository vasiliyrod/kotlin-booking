--liquibase formatted sql

--changeset vrodmanich:create-constraint-for-booking-create

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE booking ADD CONSTRAINT booking_no_overlap
EXCLUDE USING gist (
    b_room WITH =,
    tstzrange(start_date, end_date, '[)') WITH &&
);
