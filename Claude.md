# Hero Clash — Project Spec

Real-time multiplayer superhero-stat card battle game. Built as a portfolio/resume project to demonstrate scalable, room-based real-time architecture. **Not affiliated with Marvel/DC — use generic branding, no official artwork or trademarked names in the deployed app.**

## Tech Stack
- **Backend:** Java 21, Spring Boot 3.x, Maven
- **Real-time:** Spring WebSocket + STOMP over SockJS. Use `enableStompBrokerRelay` with RabbitMQ (not the in-memory SimpleBroker) so broadcasts work correctly across multiple backend instances.
- **Shared state:** Redis for live room/game state (so any instance can serve any room)
- **Persistence:** PostgreSQL (cards, users, match history)
- **Frontend:** React + `@stomp/stompjs` + `sockjs-client`
- **Card data:** [akabab/superhero-api](https://akabab.github.io/superhero-api/api/) — free static JSON, 731 characters, `powerstats`: intelligence, strength, speed, durability, power, combat (0–100). Fetch once, seed into Postgres on startup.
- **Deployment:** Docker Compose (app, postgres, redis, rabbitmq), GitHub Actions CI (build + test)

## Package Structure
```
src/main/java/com/herobattle/
├── config/          WebSocketConfig, RedisConfig
├── controller/      RoomController (REST), GameSocketController (@MessageMapping)
├── model/           Card, Player, Room, GameSession
├── repository/      CardRepository, RoomRepository
└── service/         RoomService, GameEngine, CardSeedService
```

## Core Entities
- **Card** — id, name, alignment, imageUrl, intelligence, strength, speed, durability, power, combat
- **Player** — id, displayName, sessionId, roomId
- **Room** — id (room code), status, players[], per-player deck state
- **GameSession** — roomId, currentRound, activePlayerId, roundCap

## Game Rules — "Hero Clash" MVP
1. **Deal:** shuffle full card pool, deal evenly face-down to each player (2–4 players per room)
2. **Pick category:** active player picks one stat (Intelligence/Strength/Speed/Durability/Power/Combat) based on their own top card
3. **Reveal:** all players flip their top card; server holds values until everyone has responded, then reveals simultaneously
4. **Resolve:** highest value takes all played cards (added to bottom of winner's deck). Ties → tied players flip their next card on the same stat ("showdown") until one winner takes the pot
5. **Next round:** round winner becomes the new active player
6. **Match ends:** a player runs out of cards (eliminated; last player standing wins), OR a round cap is hit (default 25 rounds — most cards held wins). Keep the cap; it bounds match length for server/room cleanup.

## Recommended Build Order (feed to Claude Code one phase at a time)
1. Scaffold Maven project, base entities, Postgres config
2. `CardSeedService` — fetch akabab dataset, populate `cards` table
3. `WebSocketConfig` (STOMP + SockJS) + `RoomController` (create/join room REST)
4. `RoomService` + Redis-backed room state
5. `GameEngine` — round resolution, tie-break, win condition, with unit tests
6. `GameSocketController` — wire `GameEngine` to STOMP message handlers
7. React frontend — room join screen, card/battle view, STOMP client
8. Docker Compose + RabbitMQ broker relay (horizontal scaling proof)
9. GitHub Actions CI (build + test)
10. Stretch: Battle Mode v2 (HP from durability+strength, turn order from power/combat/speed), chat system