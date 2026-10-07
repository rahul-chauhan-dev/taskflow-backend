# TaskFlow (Backend)

![CI](https://github.com/rahul-chauhan-dev/taskflow-backend/actions/workflows/ci.yml/badge.svg)

Spring Boot REST API for TaskFlow, a project and task manager.
Frontend repository: https://github.com/YOUR_USERNAME/taskflow-frontend

## Features
- JWT authentication with BCrypt-hashed passwords, USER and ADMIN roles
- Projects, tasks and comments, with search, filters, sorting and pagination
- Each user can only read and change their own data (other users' data returns 404)
- Dashboard statistics from aggregate queries
- Consistent JSON error format with field-level validation errors

## Tech stack
Java, Spring Boot, Spring Security, Spring Data JPA, MySQL (H2 for tests),
JUnit 5, Mockito, MockMvc

## Run locally
1. Create the database: `CREATE DATABASE taskflow;`
2. Create `src/main/resources/application-local.properties` (not committed):
```properties
   spring.datasource.password=your_password
```
3. Run with the `local` profile:
   `mvn spring-boot:run -Dspring-boot.run.profiles=local`

The API runs on http://localhost:8080.

## Tests
```bash
mvn test
```
Unit tests cover the JWT, auth and project services. Integration tests start the
application on an in-memory database and check authentication, validation, the error
format, pagination, the dashboard numbers, and that one user can never read or change
another user's data.

## Configuration
| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Database connection |
| `JWT_SECRET` | Token signing key, at least 32 bytes |
| `CORS_ORIGINS` | Allowed frontend origins, comma-separated |

## Design notes
- Controller, service and repository layers, with DTOs instead of exposed entities
- Ownership is checked on every endpoint, so missing and foreign resources look identical
- Fetch joins and batched count queries avoid N+1 queries

## Known limitations
- JWTs can't be revoked before they expire
- No rate limiting, email verification or password reset