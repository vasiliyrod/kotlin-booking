--liquibase formatted sql

--changeset vrodmanich:create-roles-for-guest

CREATE TABLE roles (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

INSERT INTO roles (id, name) VALUES
    (1, 'USER'),
    (2, 'ADMIN');

ALTER TABLE guest
ADD COLUMN role_id INTEGER;

ALTER TABLE guest
ADD CONSTRAINT fk_guest_role
FOREIGN KEY (role_id) REFERENCES roles(id);

--changeset vrodmanich:make-phone-number-unique-and-not-null
ALTER TABLE guest
ALTER COLUMN phone_number SET NOT NULL;

ALTER TABLE guest
ADD CONSTRAINT guest_phone_number_unique UNIQUE (phone_number);

--changeset vrodmanich:create-password-for-guest
ALTER TABLE guest
ADD COLUMN password_hash TEXT;

--changeset vrodmanich:create-refresh-token-table
CREATE TABLE refresh_token (
    id          SERIAL PRIMARY KEY,
    guest_id     UUID NOT NULL,
    token       TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_token_guest FOREIGN KEY (guest_id) REFERENCES guest (id) ON DELETE CASCADE
);
