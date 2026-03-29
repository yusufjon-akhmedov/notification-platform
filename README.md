# Notification Delivery Platform

Spring Boot 3.5 / Java 21 backend for managing notification templates, creating notifications, and securing the API with JWT authentication.

## Implemented Core

- JWT-based authentication with stateless Spring Security
- Register and login endpoints
- Protected template CRUD APIs
- Protected notification create/list/get/cancel APIs
- Role-based access for `ADMIN`, `MANAGER`, and `OPERATOR`
- Swagger UI / OpenAPI documentation with Bearer token support
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

## Swagger / OpenAPI

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- OpenAPI YAML: `http://localhost:8081/v3/api-docs.yaml`

Swagger UI supports JWT Bearer authentication for protected endpoints.

1. Login using `POST /api/auth/login`.
2. Copy the returned JWT access token.
3. Open Swagger UI and click `Authorize`.
4. Enter `Bearer <accessToken>` and authorize requests.

## Local Run

Requirements:

- Java 21
- Maven
- PostgreSQL

Default application settings:

- Server port: `8081`
- Database URL: `jdbc:postgresql://localhost:5434/notification_platform`
- Database username: `postgres`
- Database password: `postgres`
- JWT secret env override: `JWT_SECRET`
- JWT expiration env override: `JWT_EXPIRATION`
- Docker Compose PostgreSQL port mapping: `5434:5432`

Start locally:

```bash
docker compose up -d
mvn spring-boot:run
```

Compile check:

```bash
mvn -q -DskipTests compile
```

## Docker Compose Run

Run the full stack with Docker:

```bash
docker compose up --build
```

Services:

- App: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- PostgreSQL host port: `5434`

Inside Docker Compose, the app connects to PostgreSQL with:

- JDBC URL: `jdbc:postgresql://postgres:5432/notification_platform`
- Username: `postgres`
- Password: `postgres`
