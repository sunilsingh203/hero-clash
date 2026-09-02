# Hero Clash

Real-time multiplayer superhero-stat card battle game. See `CLAUDE.md` for the full
project spec and architecture.

## Local development

Prerequisites: JDK 21, Maven 3.9+, Docker.

```bash
# 1. Start Postgres
docker compose up -d

# 2. Run the app (schema is auto-created on first start)
mvn spring-boot:run

# 3. Tear down
docker compose down          # keep data
docker compose down -v       # also drop the volume
```

The app connects to `localhost:5432` using the credentials in
`src/main/resources/application.yml`, which match the env vars in `docker-compose.yml`
(db / user / password all `heroclash`).

Running `mvn spring-boot:run` without `docker compose up -d` first will fail fast with a
connection error — this is expected.

## Build & test

```bash
mvn clean install    # full build
mvn test             # repository tests run against in-memory H2 (no Docker needed)
```

## Schema management strategy

This project uses Hibernate `ddl-auto=update` (not Flyway) for now. Tradeoff: fast
iteration while entities are still being shaped across early feature branches, at the
cost of no versioned/reversible migration history. Flyway should be introduced no later
than the first feature that deploys to a shared/persistent environment.
