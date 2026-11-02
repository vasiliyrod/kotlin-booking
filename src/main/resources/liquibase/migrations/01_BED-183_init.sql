--liquibase formatted sql

--changeset vrodmanich:complete-guest-table
CREATE TABLE guest
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    surname      TEXT,
    firstname    TEXT,
    lastname     TEXT,
    birthdate    DATE,
    phone_number TEXT --VARCHAR(20);
);

--changeset vrodmanich:create-room-table
CREATE TABLE room
(
    id       SERIAL PRIMARY KEY,
    floor    INTEGER     NOT NULL,
    number   TEXT NOT NULL,
    capacity INTEGER,
    CONSTRAINT unique_room_floor_number UNIQUE (floor, number)
);

--changeset vrodmanich:create-booking-table
CREATE TABLE booking
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    start_date TIMESTAMPTZ NOT NULL,
    end_date   TIMESTAMPTZ NOT NULL,
    b_room     INTEGER     NOT NULL,
    CONSTRAINT fk_booking_room FOREIGN KEY (b_room) REFERENCES room (id)
);

--changeset vrodmanich:create-booking_guests-table
CREATE TABLE booking_guests
(
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    bg_guest   uuid NOT NULL,
    bg_booking uuid NOT NULL,
    CONSTRAINT fk_bg_guest FOREIGN KEY (bg_guest) REFERENCES guest (id) ON DELETE CASCADE,
    CONSTRAINT fk_bg_booking FOREIGN KEY (bg_booking) REFERENCES booking (id) ON DELETE CASCADE,
    CONSTRAINT unique_bg_guest_booking UNIQUE (bg_guest, bg_booking)
);