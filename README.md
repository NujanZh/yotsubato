# Yotsubato

[![CI](https://github.com/NujanZh/yotsubato/actions/workflows/ci.yml/badge.svg)](https://github.com/NujanZh/yotsubato/actions/workflows/ci.yml)

Yotsubato is a real-time messaging backend built with Spring Boot. It provides REST APIs for authentication, rooms,
memberships, join requests, and message history, plus STOMP over WebSocket for live room events.

This project is still in active development. I am building it as a resume project to practice production-style backend
design: authentication, authorization, persistence, migrations, integration testing, and real-time messaging behavior.

## Features

- JWT authentication with short-lived access tokens and Redis-backed refresh tokens
- User registration, login, and token refresh
- Public, private, and direct message rooms
- Room membership management with admin/member roles
- Join request flow for private rooms
- Message creation over STOMP WebSocket
- Message editing with sender-only authorization
- Message history over REST with cursor pagination
- Soft deletion of messages with room event broadcasts
- Real-time typing indicators over STOMP
- Room-scoped WebSocket authorization
- Expired-token checks for existing WebSocket sessions
- RFC 9457-style `ProblemDetail` error responses for REST endpoints
- Database migrations with Flyway
- Integration tests using Testcontainers
- GitHub Actions CI for formatting and tests

## Tech Stack

- Java 21
- Spring Boot 4
- MariaDB
- Redis
- Flyway
- Docker Compose

## Architecture

The backend is organized around a small layered structure:

```text
controller     REST and STOMP entry points
service        business rules and transactions
repository     Spring Data persistence
model          JPA entities
dto            API request/response contracts
security       JWT, refresh tokens, Spring Security config
websocket      STOMP auth, session registry, outbound filtering
web/error      ProblemDetail error responses
```

High-level request flow:

```text
REST client
  -> JwtAuthFilter
  -> Controller
  -> Service
  -> Repository
  -> MariaDB / Redis

STOMP client
  -> CONNECT with JWT
  -> StompAuthChannelInterceptor
  -> ChatController
  -> SimpMessagingTemplate
  -> RoomOutboundAuthorizationInterceptor
  -> subscribed room members
```

## WebSocket Design

Clients connect to:

```text
/api/ws
```

The `CONNECT` frame must include:

```text
Authorization: Bearer <access-token>
```

Message send destination:

```text
/app/rooms/{roomId}/message
```

Room event subscription:

```text
/topic/rooms/{roomId}
```

Room events currently include:

- `MessageCreatedEvent`
- `MessageEditedEvent`
- `MessageDeletedEvent`
- `UserTypingEvent`

The WebSocket layer does more than authenticate once on connect. It also checks room membership on subscriptions,
filters outbound room events per connected session, and rejects activity after the original JWT expires.

## REST API Overview

All endpoints are served under the `/api` context path.

Authentication:

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
```

Rooms:

```text
GET    /api/rooms
GET    /api/rooms/{id}
POST   /api/rooms
POST   /api/rooms/dm
POST   /api/rooms/{id}/join
```

Members:

```text
POST   /api/rooms/{id}/members
DELETE /api/rooms/{id}/members/me
DELETE /api/rooms/{id}/members/{userId}
PATCH  /api/rooms/{id}/members/{userId}
```

Join requests:

```text
GET    /api/rooms/{id}/join-requests
POST   /api/rooms/{id}/join-requests
POST   /api/rooms/{id}/join-requests/{reqId}/approve
POST   /api/rooms/{id}/join-requests/{reqId}/reject
DELETE /api/rooms/{id}/join-requests/{reqId}
```

Messages:

```text
GET  /api/rooms/{roomId}/messages
PATCH /api/rooms/{roomId}/messages/{messageId}
POST /api/rooms/{roomId}/messages/delete
```

## Local Development

Requirements:

- JDK 21
- Docker
- Docker Compose

Create an environment file:

```bash
cp .env.example .env
```

Example values:

```env
APP_PORT=8080

DB_PORT=3306
DB_ROOT_PASSWORD=root
DB_NAME=yotsubato
DB_USER=yotsubato
DB_PASSWORD=yotsubato

REDIS_PORT=6379
REDIS_PASSWORD=redis

JWT_PRIVATE_KEY=classpath:keys/private.pem
JWT_PUBLIC_KEY=classpath:keys/public.pem
```

Start the full stack:

```bash
docker compose up --build
```

The API will be available at:

```text
http://localhost:8080/api
```

For local development without running the app container, start only the infrastructure:

```bash
docker compose up -d mariadb redis
```

Then run the app with the `dev` profile and matching environment variables:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

## Testing

Run all tests:

```bash
./gradlew test
```

Run formatting checks:

```bash
./gradlew spotlessCheck
```

Apply formatting:

```bash
./gradlew spotlessApply
```

The integration tests start real dependencies with Testcontainers and cover flows such as authentication, room
membership, join requests, REST message edit/delete broadcasts, typing indicators, and STOMP room messaging.

## Security Notes

- Access tokens are signed with RSA keys.
- Access tokens are intentionally short-lived.
- Refresh tokens are stored in Redis and rotated on refresh.
- REST endpoints are stateless.
- WebSocket sessions store the authenticated user and token expiration time.
- Room topic broadcasts are filtered per subscribed session to avoid leaking events to removed members.

The checked-in RSA keys are suitable for local development only. Production deployments should provide their own keys
through environment-specific configuration.

## Current Status

Implemented:

- Auth flow with access and refresh tokens
- Room creation and membership management
- Private room join requests
- Direct message room creation
- WebSocket message broadcasting
- Room event filtering for removed members
- Expired-token checks for WebSocket activity
- Message history pagination
- Message editing
- Message soft deletion
- Real-time typing indicators
- Integration test coverage for main flows
- GitHub Actions CI
