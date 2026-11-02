--liquibase formatted sql

--changeset vrodmanich:insert-guests
INSERT INTO guest (id, surname, firstname, lastname, birthdate, phone_number) VALUES
                                                                                  (gen_random_uuid(), 'Иванов', 'Иван', 'Иванович', '1990-01-15', '+7 (901) 123-45-67'),
                                                                                  (gen_random_uuid(), 'Петров', 'Петр', 'Петрович', '1985-05-20', '+7 (902) 234-56-78'),
                                                                                  (gen_random_uuid(), 'Сидоров', 'Сидор', 'Сидорович', '1978-11-03', '+7 (903) 345-67-89'),
                                                                                  (gen_random_uuid(), 'Смирнова', 'Анна', 'Александровна', '1995-08-12', '+7 (904) 456-78-90'),
                                                                                  (gen_random_uuid(), 'Кузнецов', 'Дмитрий', 'Сергеевич', '1982-03-25', '+7 (905) 567-89-01'),
                                                                                  (gen_random_uuid(), 'Попова', 'Елена', 'Владимировна', '1992-07-18', '+7 (906) 678-90-12'),
                                                                                  (gen_random_uuid(), 'Васильев', 'Алексей', 'Николаевич', '1988-12-30', '+7 (907) 789-01-23'),
                                                                                  (gen_random_uuid(), 'Морозова', 'Ольга', 'Игоревна', '1975-09-05', '+7 (908) 890-12-34'),
                                                                                  (gen_random_uuid(), 'Волков', 'Андрей', 'Павлович', '1998-04-14', '+7 (909) 901-23-45'),
                                                                                  (gen_random_uuid(), 'Павлова', 'Татьяна', 'Михайловна', '1980-06-22', '+7 (910) 012-34-56');

--changeset vrodmanich:insert-rooms
INSERT INTO room (floor, number, capacity) VALUES
                                               (1, '101', 2),
                                               (1, '102', 2),
                                               (1, '103', 3),
                                               (2, '201', 2),
                                               (2, '202', 4),
                                               (2, '203', 3),
                                               (3, '301', 1),
                                               (3, '302', 2),
                                               (3, '303', 4),
                                               (3, '304', 3);

--changeset vrodmanich:insert-bookings
WITH inserted_bookings AS (
    INSERT INTO booking (start_date, end_date, b_room)
        VALUES
            ('2026-01-10 14:00:00+03', '2026-01-15 12:00:00+03', (SELECT id FROM room WHERE number = '101')),
            ('2026-02-01 14:00:00+03', '2026-02-05 12:00:00+03', (SELECT id FROM room WHERE number = '102')),
            ('2026-03-01 14:00:00+03', '2026-03-05 12:00:00+03', (SELECT id FROM room WHERE number = '201')),
            ('2026-04-01 14:00:00+03', '2026-04-04 12:00:00+03', (SELECT id FROM room WHERE number = '202')),
            ('2026-05-01 14:00:00+03', '2026-05-07 12:00:00+03', (SELECT id FROM room WHERE number = '301'))
        RETURNING id, start_date
)
INSERT INTO booking_guests (bg_guest, bg_booking)
SELECT g.id, b.id
FROM inserted_bookings b
         CROSS JOIN LATERAL (
    SELECT id FROM guest
    WHERE
        (b.start_date = '2026-01-10 14:00:00+03' AND surname IN ('Иванов', 'Петров'))
       OR (b.start_date = '2026-02-01 14:00:00+03' AND surname IN ('Сидоров', 'Смирнова'))
       OR (b.start_date = '2026-03-01 14:00:00+03' AND surname IN ('Кузнецов', 'Попова', 'Васильев'))
       OR (b.start_date = '2026-04-01 14:00:00+03' AND surname IN ('Морозова', 'Волков'))
       OR (b.start_date = '2026-05-01 14:00:00+03' AND surname IN ('Павлова', 'Иванов', 'Петров'))
    ) g;