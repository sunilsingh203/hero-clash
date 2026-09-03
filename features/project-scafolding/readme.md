# Feature: Project Scaffold & Data Model

## Goal
Stand up a working Spring Boot + Maven project that compiles, connects to PostgreSQL, and defines the core data entities — no game logic, no REST endpoints, no WebSocket yet. This is the foundation every later feature builds on.

## Context
Part of the Hero Clash project (see the project's `CLAUDE.md` for full stack and architecture). This is feature #1 in the build order.

## Requirements
1. Maven project (Java 21, Spring Boot 3.x) that builds cleanly with `mvn clean install`.
2. Spring Data JPA + PostgreSQL driver, configured via `application.yml`, connecting to a local Postgres instance.
3. Package structure matches `CLAUDE.md`: `config/`, `controller/`, `model/`, `repository/`, `service/`.
4. Entities:
    - **Card** — id (Long, auto), name (String), alignment (String), imageUrl (String), intelligence / strength / speed / durability / power / combat (Integer, 0–100)
    - **Player** — id (Long, auto), displayName (String), sessionId (String), roomId (FK → Room)
    - **Room** — id (Long, auto), code (String, unique, e.g. 6-character room code), status (enum: `WAITING`, `IN_PROGRESS`, `FINISHED`)
5. A Spring Data JPA repository interface for each entity (`CardRepository`, `PlayerRepository`, `RoomRepository`), including at least one derived query method (e.g. `findByCode` on `RoomRepository`).
6. Schema management strategy chosen and documented (Flyway migration, or Hibernate `ddl-auto=update` for now — pick one and note the tradeoff).
7. A minimal `docker-compose.yml` with a `postgres` service, so the project runs locally via `docker compose up -d` followed by `mvn spring-boot:run`.
8. App starts cleanly against an empty database with no errors.

## Out of scope
- No REST endpoints or controllers
- No WebSocket configuration
- No card data seeding (feature #2)
- No game logic (feature #6)
- No authentication

## Acceptance criteria
- `mvn clean install` passes.
- `docker compose up -d` brings up Postgres; the Spring Boot app connects on startup with no errors.
- The three entity classes and three repository interfaces exist, matching the schema above.
- At least one repository test passes (e.g., save and fetch a `Room`).