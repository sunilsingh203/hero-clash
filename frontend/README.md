# Hero Clash — Frontend

React + Vite client for the Hero Clash backend. Talks REST for room lifecycle and
STOMP-over-SockJS (`@stomp/stompjs` + `sockjs-client`) for live gameplay.

## Run

```bash
npm install
npm run dev        # http://localhost:5173, proxies /api and /ws to :8080
```

The backend must be running on `:8080` (see the root README).

## Structure

| Path | Purpose |
| --- | --- |
| `src/api.js` | REST client (create/join room, read game + hand) |
| `src/useGameSocket.js` | one STOMP connection per room; exposes `start/pick/reveal` + latest `GameView` |
| `src/components/Home.jsx` | create or join a room |
| `src/components/Lobby.jsx` | player list, start the match |
| `src/components/Battle.jsx` | the table: opponents, stage, your hand |
| `src/components/HeroCard.jsx` | a card with stat bars; face-down variant |

## Flow

1. `Home` → `POST /api/rooms` (+ `/players`) → store `{code, playerId}` in `localStorage`.
2. `Lobby` polls `GET /api/rooms/{code}` for joins; **Start** sends `/app/rooms/{code}/start`.
3. `Battle` renders each `GameView` pushed to `/topic/rooms/{code}`; the active player picks
   a stat, contenders press **Reveal**, and the `resolution` block shows who took the pot.
