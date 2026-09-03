# Plan: Project Scaffold & Data Model

Ordered, independently testable tasks implementing `spec.md`. No implementation code is written in this planning step.

---

### Task 1 — Initialize Maven project skeleton

**Files:** `pom.xml`, `src/main/java/com/herobattle/HeroClashApplication.java`, `src/main/resources/application.yml`, `.gitignore`

**Accomplishes:** Create a Spring Boot 3.x / Java 21 Maven project with `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, PostgreSQL driver, and `spring-boot-starter-test` dependencies. Add a minimal `@SpringBootApplication` main class. Add a placeholder `application.yml` (datasource block filled in Task 2). Add a `.gitignore` for `target/`, IDE files.

**Verify:** `mvn -q clean compile` succeeds with no errors.

---

### Task 2 — Configure Postgres datasource and JPA properties

**Files:** `src/main/resources/application.yml`

**Accomplishes:** Add `spring.datasource.url/username/password/driver-class-name` pointing at `localhost:5432` for a local Postgres instance, plus `spring.jpa.hibernate.ddl-auto=update` and `spring.jpa.show-sql=true`. Values must match the credentials used in the `docker-compose.yml` created in Task 7.

**Verify:** Manual check — file contains all required keys; `mvn clean install` still compiles (no live DB needed to compile). Full connectivity verified in Task 8.

---

### Task 3 — Create `Room` entity

**Files:** `src/main/java/com/herobattle/model/Room.java`, `src/main/java/com/herobattle/model/RoomStatus.java`

**Accomplishes:** JPA `@Entity` with `id` (Long, `@Id @GeneratedValue`), `code` (String, `@Column(unique = true, length = 6, nullable = false)`), `status` (enum `RoomStatus { WAITING, IN_PROGRESS, FINISHED }`, `@Enumerated(EnumType.STRING)`, not null).

**Verify:** `mvn -q clean compile` succeeds. Class matches field spec in `spec.md` §4.

---

### Task 4 — Create `Player` entity

**Files:** `src/main/java/com/herobattle/model/Player.java`

**Accomplishes:** JPA `@Entity` with `id` (Long, auto), `displayName` (String), `sessionId` (String), and `@ManyToOne` relationship to `Room` via `room_id` FK column (nullable, per spec edge case #4).

**Verify:** `mvn -q clean compile` succeeds; field names/types match spec.

---

### Task 5 — Create `Card` entity

**Files:** `src/main/java/com/herobattle/model/Card.java`

**Accomplishes:** JPA `@Entity` with `id` (Long, auto), `name`, `alignment`, `imageUrl` (String), and `intelligence`, `strength`, `speed`, `durability`, `power`, `combat` (Integer, no validation annotations per spec — deferred to seeding feature).

**Verify:** `mvn -q clean compile` succeeds; field names/types match spec §4 table exactly (including `image_url` column naming via default Hibernate snake_case strategy).

---

### Task 6 — Create repository interfaces

**Files:** `src/main/java/com/herobattle/repository/CardRepository.java`, `PlayerRepository.java`, `RoomRepository.java`

**Accomplishes:** Three `JpaRepository<Entity, Long>` interfaces. `RoomRepository` additionally declares `Optional<Room> findByCode(String code)`.

**Verify:** `mvn -q clean compile` succeeds; signatures match spec §3 exactly.

---

### Task 7 — Write `docker-compose.yml` for local Postgres

**Files:** `docker-compose.yml`

**Accomplishes:** Define a `postgres` service (official `postgres` image, e.g. `postgres:16`), exposing `5432:5432`, a named volume for data persistence, and `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` env vars matching `application.yml` from Task 2.

**Verify:** `docker compose up -d` starts a healthy container; `docker compose ps` shows it running; `docker compose down` cleans up. (Manual check — no automated test.)

---

### Task 8 — Verify end-to-end clean startup

**Files:** none (verification-only task; may touch `README.md` to document the run steps)

**Accomplishes:** Confirm the full local dev loop works: `docker compose up -d` → `mvn spring-boot:run` → app starts against an empty DB with Hibernate auto-creating `card`, `room`, `player` tables and no errors in logs.

**Verify:** Manual check — app log shows `Started HeroClashApplication` with no exceptions; `psql`/DB client shows the three tables created with expected columns; `docker compose down` afterward.

---

### Task 9 — Repository round-trip test

**Files:** `src/test/java/com/herobattle/repository/RoomRepositoryTest.java` (and `src/test/resources/application.yml` or test-specific config if an isolated test DB/Testcontainers is used)

**Accomplishes:** A `@DataJpaTest` (or Testcontainers-backed) test that saves a `Room` with a given `code`/`status`, then fetches it via `findByCode`, asserting the returned entity matches what was saved.

**Verify:** `mvn -q test` passes, specifically this test class; test fails if `findByCode` or entity mapping is broken (sanity-checked by temporarily breaking the query, confirming failure, then reverting — optional manual step, not required for sign-off).

---

### Task 10 — Document schema strategy decision

**Files:** `README.md` (project root or feature folder) or a code comment in `application.yml`

**Accomplishes:** One-line documented rationale for choosing Hibernate `ddl-auto=update` over Flyway for this feature, per spec §2 requirement 10 and §4.

**Verify:** Manual check — rationale is present and readable in-repo.

---

## Task ordering rationale

Tasks 1–2 establish the buildable base and config before any entity code exists (so `mvn compile` is meaningful at every subsequent step). Tasks 3–5 add entities independently (any order among themselves would work, but `Room` first since `Player` depends on it via FK). Task 6 depends on all three entities existing. Task 7 (Docker) is independent of the Java code and could run in parallel, but is sequenced after repositories so Task 8's end-to-end check has everything in place. Task 9 (test) depends on Task 6 (repository) and Task 7/8 (a reachable DB, if using a real Postgres in tests) or can use an in-memory/Testcontainers approach independent of Task 7. Task 10 is a lightweight documentation wrap-up.
