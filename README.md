# Web приложение для бронирования отелей

REST API для бронирования номеров в отеле: регистрация гостей, поиск свободных комнат на даты, создание и изменение бронирований, разграничение доступа по ролям (PUBLIC / USER / ADMIN).

Написано на **Kotlin** без веб-фреймворка — HTTP-слой построен вручную поверх встроенного в JDK `com.sun.net.httpserver` (свой `Router` и `DispatcherHandler`). Данные хранятся в **PostgreSQL**, доступ к БД — через **Spring JDBC** с пулом соединений **HikariCP**, миграции — **Liquibase**. Аутентификация — **JWT** с парами access/refresh токенов (refresh передаётся через `httpOnly` cookie). Сериализация JSON — **kotlinx.serialization**, конфигурация — через `.env` (dotenv-kotlin), логирование — **logback** + kotlin-logging. Тесты — юнит (MockK) и интеграционные с реальной БД в **Testcontainers**.

## Запуск

1. Добавить в корень проекта файл `.env`
2. Заполнить файл данными конфигурации. Пример:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/bookings
DB_USER=postgres
DB_PASSWORD=admin
JWT_SECRET_KEY=mySuperSecretKeyForJwtThatIsAtLeast32BytesLong
ACCESS_TOKEN_TTL_MS=900000
REFRESH_TOKEN_TTL_DAYS=30
```

3. Запустить программу

```bash
./gradlew run
```

4. К приложению можно обращаться по адресу `http://localhost:8080`

## Доступные методы REST API

### Аутентификация (Auth)

Эндпоинты не требуют токена доступа. Refresh token передаётся через `httpOnly` cookie.

| Метод   | Эндпоинт                 | Описание                         | Доступ      | Тело запроса            | Ответ                                       |
|---------|--------------------------|----------------------------------|-------------|-------------------------|---------------------------------------------|
| **POST** | `auth/register`          | Регистрация нового пользователя  | PUBLIC      | `GuestCreateDto`        | `RegisterResultDto` (гость + access token)  |
| **POST** | `auth/login`             | Вход в систему                   | PUBLIC      | `AuthLoginDto`          | `AccessTokenDto` + `refresh_token` cookie   |
| **POST** | `auth/logout`            | Выход (удаление refresh token)   | USER        | —                       | `SuccessResponseDto`                        |
| **POST** | `auth/logout-from-all`   | Выход со всех устройств          | USER        | —                       | `SuccessResponseDto`                        |
| **POST** | `auth/refresh`           | Обновление access token          | USER        | `refresh_token` cookie  | `AccessTokenDto`                            |
| **PUT**  | `auth/change-password`   | Смена пароля                     | USER        | `AuthChangePasswordDto` | `SuccessResponseDto`                        |

---

### Комнаты (Rooms)

| Метод   | Эндпоинт    | Описание                             | Доступ   | Тело запроса    | Ответ                     |
|---------|-------------|--------------------------------------|----------|-----------------|---------------------------|
| **GET** | `room/:id`  | Получить комнату по ID               | USER     | —               | `RoomReadDto`             |
| **POST**| `room`      | Создать комнату                      | ADMIN    | `RoomCreateDto`  | `RoomReadDto`             |
| **PUT** | `room`      | Обновить комнату                     | ADMIN    | `RoomEditDto`    | `RoomReadDto`             |

---

### Гости (Guests)

| Метод   | Эндпоинт      | Описание                       | Доступ   | Тело запроса         | Ответ                |
|---------|---------------|--------------------------------|----------|----------------------|----------------------|
| **GET** | `guest/:uuid` | Получить гостя по UUID         | ADMIN    | —                    | `GuestReadDto`       |
| **PUT** | `guest`       | Обновить свои данные           | USER     | `GuestEditDto`       | `GuestReadDto`       |
| **PUT** | `guest/role`  | Изменить роль гостя            | ADMIN    | `GuestChangeRoleDto` | `SuccessResponseDto` |

---

### Бронирования (Bookings)

| Метод   | Эндпоинт                     | Описание                          | Доступ   | Тело запроса               | Ответ                       |
|---------|------------------------------|-----------------------------------|----------|----------------------------|-----------------------------|
| **GET** | `booking/:uuid`              | Получить бронирование по UUID     | USER     | —                          | `BookingReadDto`            |
| **GET** | `booking/available-rooms`    | Найти доступные комнаты           | USER     | `BookingAvailableRoomsDto` | `List<RoomReadDto>`         |
| **POST**| `booking`                    | Создать бронирование              | USER     | `BookingCreateDto`         | `BookingReadDto`            |
| **PUT** | `booking`                    | Обновить бронирование             | USER     | `BookingEditDto`           | `BookingReadDto`            |

---

### Форматы DTO

#### Auth

##### `GuestCreateDto`

```json
{
  "surname": "Иванов",
  "firstname": "Иван",
  "lastname": "Иванович",
  "birthdate": "2000-01-15",
  "phoneNumber": "+71234567890",
  "password": "securePassword123"
}
```

##### `AuthLoginDto`

```json
{
  "phoneNumber": "+71234567890",
  "password": "securePassword123"
}
```

##### `AuthChangePasswordDto`

```json
{
  "phoneNumber": "+71234567890",
  "oldPassword": "oldPass123",
  "newPassword": "newPass456"
}
```

##### `AccessTokenDto`

```json
{
  "accessToken": "eyJhbGciOi...",
  "message": "Successfully logged in"
}
```

##### `RegisterResultDto`

```json
{
  "guestReadDto": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "surname": "Иванов",
    "firstname": "Иван",
    "lastname": "Иванович",
    "birthdate": "2000-01-15",
    "phoneNumber": "+71234567890",
    "role": "USER"
  },
  "accessTokenDto": {
    "accessToken": "eyJhbGciOi...",
    "message": "Successfully created access token"
  }
}
```

##### `SuccessResponseDto`

```json
{
  "message": "Password changed successfully"
}
```

#### Rooms

##### `RoomCreateDto`

```json
{
  "floor": 2,
  "number": "101A",
  "capacity": 4
}
```

##### `RoomEditDto`

```json
{
  "id": 1,
  "floor": 3,
  "number": "202B",
  "capacity": 6
}
```

##### `RoomReadDto`

```json
{
  "id": 1,
  "floor": 2,
  "number": "101A",
  "capacity": 4
}
```

#### Guests

##### `GuestEditDto`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "surname": "Петров",
  "firstname": "Пётр",
  "birthdate": "1995-05-20",
  "phoneNumber": "+70987654321"
}
```

##### `GuestChangeRoleDto`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "roleId": 2
}
```

##### `GuestReadDto`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "surname": "Иванов",
  "firstname": "Иван",
  "lastname": "Иванович",
  "birthdate": "2000-01-15",
  "phoneNumber": "+71234567890",
  "role": "USER"
}
```

#### Bookings

##### `BookingCreateDto`

```json
{
  "startDate": "2026-07-01T14:00:00",
  "endDate": "2026-07-05T12:00:00",
  "roomId": 1,
  "guestIds": [
    "550e8400-e29b-41d4-a716-446655440000"
  ]
}
```

##### `BookingEditDto`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "startDate": "2026-07-01T14:00:00",
  "endDate": "2026-07-07T12:00:00",
  "roomId": 2,
  "guestIds": [
    "550e8400-e29b-41d4-a716-446655440000",
    "660e8400-e29b-41d4-a716-446655440002"
  ]
}
```

##### `BookingAvailableRoomsDto`

```json
{
  "startDate": "2026-07-01T14:00:00",
  "endDate": "2026-07-05T12:00:00"
}
```

##### `BookingReadDto`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "startDate": "2026-07-01T14:00:00",
  "endDate": "2026-07-05T12:00:00",
  "room": {
    "id": 1,
    "floor": 2,
    "number": "101A",
    "capacity": 4
  },
  "guests": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "surname": "Иванов",
      "firstname": "Иван",
      "lastname": "Иванович",
      "birthdate": "2000-01-15",
      "phoneNumber": "+71234567890",
      "role": "USER"
    }
  ]
}
```

#### Error

```json
{
  "error": "Этаж должен быть в диапазоне от -100 до 300, Номер комнаты не может быть пустым"
}
```

### Роли

| ID | Название | Описание                    |
|----|----------|-----------------------------|
| 0  | PUBLIC   | Любой человек (без токена)  |
| 1  | USER     | Авторизованный пользователь |
| 2  | ADMIN    | Администратор               |

## Запуск тестов

1. **Docker Desktop** должен быть установлен и запущен
2. Установить **PostgreSQL образ**:

```bash
docker pull postgres:18
```

3. Запустить тесты

```bash
./gradlew test
```

## Структура проекта

```plaintext
src
├── main
│   ├── kotlin
│   │   ├── controller              # HTTP-контроллеры 
│   │   │   ├── AuthController.kt
│   │   │   ├── BaseController.kt
│   │   │   ├── BookingController.kt
│   │   │   ├── DispatcherHandler.kt # Класс отправляющий запросы в Router
│   │   │   ├── GuestController.kt
│   │   │   ├── RoomController.kt
│   │   │   └── Router.kt           # Класс распределяющий запросы по конроллерам
│   │   ├── data
│   │   │   ├── entity              # Сущности БД
│   │   │   └── repository          # DAO-слой
│   │   ├── dto                     # Data Transfer Objects
│   │   │   ├── auth
│   │   │   ├── booking
│   │   │   ├── guest
│   │   │   ├── room
│   │   │   ├── serializers
│   │   │   ├── OperationResult.kt # класс- результаты
│   │   │   ├── SuccessResponseDto.kt
│   │   │   └── ValidationResult.kt
│   │   ├── exception              # Кастомные исключения
│   │   ├── mapper                 # Мапперы Entity - DTO
│   │   ├── service                # Бизнес-логика
│   │   ├── utils                  # Утилиты
│   │   │   ├── AppConfig.kt
│   │   │   ├── DataBaseConfig.kt
│   │   │   ├── FunctionalTypes.kt
│   │   │   ├── LiquibaseUtil.kt
│   │   │   └── PasswordUtil.kt
│   │   └── Main.kt                # Точка входа
│   │
│   └── resources
│       ├── liquibase              # Миграции БД (Liquibase)
│       │   ├── migrations
│       │   └── changelog-master.yaml
│       └── logback.xml            # Конфиг логирования
│
└── test
    └── kotlin
        ├── integration            # Интеграционные тесты
        └── unit
            └── validation         # Юнит тесты валидации
```