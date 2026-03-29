# Notification Delivery Platform

Spring Boot 3.5 / Java 21 backend for managing notification templates, creating notifications, and securing the API with JWT authentication.

## Implemented Core

- JWT-based authentication with stateless Spring Security
- Register and login endpoints
- Protected template CRUD APIs
- Protected notification create/list/get/cancel APIs
- Role-based access for `ADMIN`, `MANAGER`, and `OPERATOR`
- Flyway-based schema management with Hibernate validation

## Main Endpoints

### Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`

### Templates

- `POST /api/templates`
- `GET /api/templates`
- `GET /api/templates/{id}`
- `PUT /api/templates/{id}`
- `DELETE /api/templates/{id}`

### Notifications

- `POST /api/notifications`
- `GET /api/notifications`
- `GET /api/notifications/{id}`
- `PATCH /api/notifications/{id}/cancel`

## Auth Flow

1. Register a user with `POST /api/auth/register`.
2. Login with `POST /api/auth/login`.
3. Read the `accessToken` from the login response.
4. Call protected endpoints with `Authorization: Bearer <accessToken>`.

## Local Run

Requirements:

- Java 21
- Maven
- PostgreSQL

Default application settings:

- Server port: `8081`
- Database URL: `jdbc:postgresql://localhost:5432/notification_platform`
- Database username: `postgres`
- Database password: `postgres`
- JWT secret env override: `JWT_SECRET`
- JWT expiration env override: `JWT_EXPIRATION`

Start locally:

```bash
mvn spring-boot:run
```

Compile check:

```bash
mvn -q -DskipTests compile
```

Note:

- The repository includes `compose.yaml`, but its PostgreSQL password is currently `secret` while the application config expects `postgres`. Make those values consistent before running with Docker Compose.
